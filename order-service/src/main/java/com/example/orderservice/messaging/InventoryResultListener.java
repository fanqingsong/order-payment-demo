/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryResultListener.java                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】消费 inventory-reserved / inventory-reservation-failed。
 * 【为什么】把库存 Saga 结果写回 order_db。
 * 【怎么做】委托 InventoryResultService.handle*。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   inventory-* ──► InventoryResultService
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.messaging;

import com.example.orderservice.event.InventoryReservationFailedEvent;
import com.example.orderservice.event.InventoryReservedEvent;
import com.example.orderservice.service.InventoryResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryResultListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryResultListener.class);

    private final InventoryResultService inventoryResultService;

    public InventoryResultListener(InventoryResultService inventoryResultService) {
        this.inventoryResultService = inventoryResultService;
    }

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVED,
            properties = "spring.json.value.default.type=com.example.orderservice.event.InventoryReservedEvent"
    )
    public void onInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent: eventId={}, orderId={}", event.eventId(), event.orderId());
        inventoryResultService.handleReserved(event);
    }

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVATION_FAILED,
            properties = "spring.json.value.default.type=com.example.orderservice.event.InventoryReservationFailedEvent"
    )
    public void onInventoryReservationFailed(InventoryReservationFailedEvent event) {
        log.info("Received InventoryReservationFailedEvent: eventId={}, orderId={}",
                event.eventId(), event.orderId());
        inventoryResultService.handleReservationFailed(event);
    }
}
