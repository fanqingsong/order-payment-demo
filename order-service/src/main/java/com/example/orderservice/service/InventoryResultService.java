/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryResultService.java                                ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】根据库存 Kafka 事件更新 Order 状态。
 * 【为什么】连接 inventory Saga 与 order 聚合。
 * 【怎么做】@Transactional handleReserved/Failed；仅 PENDING_INVENTORY 可迁移。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   inventory event ──► order.status
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.InventoryReservationFailedEvent;
import com.example.orderservice.event.InventoryReservedEvent;
import com.example.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class InventoryResultService {

    private static final Logger log = LoggerFactory.getLogger(InventoryResultService.class);

    private final OrderRepository orderRepository;

    public InventoryResultService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void handleReserved(InventoryReservedEvent event) {
        updateOrder(event.eventId(), event.orderId(), true);
    }

    @Transactional
    public void handleReservationFailed(InventoryReservationFailedEvent event) {
        updateOrder(event.eventId(), event.orderId(), false);
    }

    private void updateOrder(UUID eventId, UUID orderId, boolean reserved) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.error("Cannot apply inventory result because order was not found: eventId={}, orderId={}",
                    eventId, orderId);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING_INVENTORY) {
            log.warn("Ignoring inventory result for non-pending-inventory order: eventId={}, orderId={}, status={}",
                    eventId, orderId, order.getStatus());
            return;
        }

        Instant now = Instant.now();
        if (reserved) {
            order.applyInventoryReserved(now);
        } else {
            order.applyInventoryFailed(now);
        }
        log.info("Updated order from inventory result: eventId={}, orderId={}, status={}",
                eventId, orderId, order.getStatus());
    }
}
