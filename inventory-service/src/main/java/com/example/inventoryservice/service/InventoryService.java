/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryService.java                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】库存领域服务：预占、确认、释放，含幂等与演示规则（OUT_OF_STOCK 等）。
 * 【为什么】实现「先占库存再支付」，避免付了款没货。
 * 【怎么做】@Transactional 方法：幂等 → 改 Item/Reservation → ProcessedEvent → InventoryEventPublisher。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   OrderCreated
 *       │
 *       ▼
 *   reserve ──OK──► inventory-reserved
 *       └──FAIL──► reservation-failed
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.InventoryItem;
import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.entity.ProcessedEvent;
import com.example.inventoryservice.entity.ReservationStatus;
import com.example.inventoryservice.event.InventoryReservationFailedEvent;
import com.example.inventoryservice.event.InventoryReservedEvent;
import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.event.PaymentCompletedEvent;
import com.example.inventoryservice.event.PaymentFailedEvent;
import com.example.inventoryservice.messaging.InventoryEventPublisher;
import com.example.inventoryservice.repository.InventoryItemRepository;
import com.example.inventoryservice.repository.InventoryReservationRepository;
import com.example.inventoryservice.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    /** 首次见到的商品名默认可售数量（演示用；生产应通过运营维护 SKU）。 */
    private static final int DEFAULT_AVAILABLE_STOCK = 100;

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryEventPublisher eventPublisher;

    public InventoryService(
            InventoryItemRepository inventoryItemRepository,
            InventoryReservationRepository reservationRepository,
            ProcessedEventRepository processedEventRepository,
            InventoryEventPublisher eventPublisher
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.reservationRepository = reservationRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void reserveForOrder(OrderCreatedEvent orderEvent) {
        if (processedEventRepository.existsById(orderEvent.eventId())) {
            log.info("Ignoring duplicate OrderCreatedEvent: eventId={}, orderId={}",
                    orderEvent.eventId(), orderEvent.orderId());
            return;
        }

        InventoryItem item = loadOrCreateItem(orderEvent.productName());
        Instant now = Instant.now();

        if (!item.canReserve(orderEvent.quantity())) {
            processedEventRepository.save(new ProcessedEvent(orderEvent.eventId(), now));
            eventPublisher.publishReservationFailed(new InventoryReservationFailedEvent(
                    UUID.randomUUID(),
                    "InventoryReservationFailed",
                    1,
                    now,
                    orderEvent.orderId(),
                    orderEvent.productName(),
                    orderEvent.quantity(),
                    "Insufficient inventory"
            ));
            log.info("Inventory reservation failed: orderId={}, product={}, quantity={}",
                    orderEvent.orderId(), orderEvent.productName(), orderEvent.quantity());
            return;
        }

        item.reserve(orderEvent.quantity());
        inventoryItemRepository.save(item);

        UUID reservationId = UUID.randomUUID();
        reservationRepository.save(new InventoryReservation(
                reservationId,
                orderEvent.orderId(),
                orderEvent.productName(),
                orderEvent.quantity(),
                ReservationStatus.RESERVED,
                orderEvent.eventId(),
                now,
                now
        ));
        // 不会破坏幂等性：方法带 @Transactional，此处 save 尚未提交；若在后续 publish 前崩溃/中断，
        // 预占与 ProcessedEvent 会一并回滚，Kafka 重试仍可再次处理并发布。若已提交再断则另当别论（双写/Outbox）。
        processedEventRepository.save(new ProcessedEvent(orderEvent.eventId(), now));

        eventPublisher.publishReserved(new InventoryReservedEvent(
                UUID.randomUUID(),
                "InventoryReserved",
                1,
                now,
                orderEvent.orderId(),
                orderEvent.userId(),
                orderEvent.productName(),
                orderEvent.quantity(),
                orderEvent.amount(),
                orderEvent.eventId(),
                reservationId
        ));
        log.info("Inventory reserved: orderId={}, reservationId={}, product={}, quantity={}",
                orderEvent.orderId(), reservationId, orderEvent.productName(), orderEvent.quantity());
    }

    @Transactional
    public void confirmAfterPayment(PaymentCompletedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Ignoring duplicate PaymentCompletedEvent: eventId={}, orderId={}",
                    event.eventId(), event.orderId());
            return;
        }

        Instant now = Instant.now();
        applyReservationTransition(event.orderId(), true, now);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), now));
        log.info("Inventory confirmed after payment: orderId={}", event.orderId());
    }

    @Transactional
    public void releaseAfterPaymentFailure(PaymentFailedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Ignoring duplicate PaymentFailedEvent: eventId={}, orderId={}",
                    event.eventId(), event.orderId());
            return;
        }

        Instant now = Instant.now();
        applyReservationTransition(event.orderId(), false, now);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), now));
        log.info("Inventory released after payment failure: orderId={}", event.orderId());
    }

    private void applyReservationTransition(UUID orderId, boolean confirm, Instant now) {
        InventoryReservation reservation = reservationRepository.findByOrderId(orderId).orElse(null);
        if (reservation == null) {
            log.error("No inventory reservation for order: orderId={}", orderId);
            return;
        }
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            log.warn("Skipping inventory transition for reservation status {}: orderId={}",
                    reservation.getStatus(), orderId);
            return;
        }

        InventoryItem item = inventoryItemRepository.findById(reservation.getProductName())
                .orElseThrow(() -> new IllegalStateException(
                        "Inventory item missing for reservation: " + reservation.getProductName()));

        if (confirm) {
            item.confirmReservation(reservation.getQuantity());
            reservation.markConfirmed(now);
        } else {
            item.releaseReservation(reservation.getQuantity());
            reservation.markReleased(now);
        }
        inventoryItemRepository.save(item);
        reservationRepository.save(reservation);
    }

    private InventoryItem loadOrCreateItem(String productName) {
        return inventoryItemRepository.findById(productName)
                .orElseGet(() -> {
                    int available = "OUT_OF_STOCK".equals(productName) ? 0 : DEFAULT_AVAILABLE_STOCK;
                    return inventoryItemRepository.save(new InventoryItem(productName, available, 0));
                });
    }
}
