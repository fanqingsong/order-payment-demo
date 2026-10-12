/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentService.java                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】处理 InventoryReserved：幂等、落 Payment、按 demo 规则发结果。
 * 【为什么】金额 ≤10000 成功，否则失败。
 * 【怎么做】@Transactional processInventoryReserved → save + publishCompleted/Failed。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   amount≤10000 ──► COMPLETED
 *   else ──► FAILED
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.entity.ProcessedEvent;
import com.example.paymentservice.event.InventoryReservedEvent;
import com.example.paymentservice.event.PaymentCompletedEvent;
import com.example.paymentservice.event.PaymentFailedEvent;
import com.example.paymentservice.messaging.PaymentEventPublisher;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 支付业务：仅在收到 {@code inventory-reserved} 后创建支付记录，并发布支付成功/失败事件。
 * <p>
 * 演示规则：订单金额 ≤ {@link #PAYMENT_LIMIT} 视为支付成功，否则失败（模拟风控/限额）。
 * 使用 {@code processed_events} 表按 {@code InventoryReservedEvent.eventId} 做<strong>消费幂等</strong>，
 * 防止 Kafka 至少一次投递导致重复扣款。
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /** 演示用支付上限：超过则 {@link PaymentStatus#FAILED}。 */
    private static final BigDecimal PAYMENT_LIMIT = new BigDecimal("10000");

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PaymentEventPublisher eventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            ProcessedEventRepository processedEventRepository,
            PaymentEventPublisher eventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processInventoryReserved(InventoryReservedEvent inventoryEvent) {
        if (processedEventRepository.existsById(inventoryEvent.eventId())) {
            log.info("Ignoring duplicate InventoryReservedEvent: eventId={}, orderId={}",
                    inventoryEvent.eventId(), inventoryEvent.orderId());
            return;
        }

        PaymentStatus status = inventoryEvent.amount().compareTo(PAYMENT_LIMIT) <= 0
                ? PaymentStatus.COMPLETED
                : PaymentStatus.FAILED;
        Instant now = Instant.now();
        Payment payment = new Payment(inventoryEvent.orderId(), inventoryEvent.amount(), status, now, now);
        Payment savedPayment = paymentRepository.save(payment);

        processedEventRepository.save(new ProcessedEvent(inventoryEvent.eventId(), Instant.now()));

        log.info("Processed payment: eventId={}, orderId={}, paymentId={}, status={}",
                inventoryEvent.eventId(), inventoryEvent.orderId(), savedPayment.getId(), savedPayment.getStatus());

        if (status == PaymentStatus.COMPLETED) {
            eventPublisher.publishCompleted(new PaymentCompletedEvent(
                    UUID.randomUUID(),
                    "PaymentCompleted",
                    1,
                    Instant.now(),
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getAmount()
            ));
        } else {
            eventPublisher.publishFailed(new PaymentFailedEvent(
                    UUID.randomUUID(),
                    "PaymentFailed",
                    1,
                    Instant.now(),
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getAmount()
            ));
        }
    }
}
