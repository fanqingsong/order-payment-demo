/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryEventPublisher.java                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】发布 inventory-reserved / inventory-reservation-failed 的组件。
 * 【为什么】在本地 DB 事务提交后通知下游，完成库存出站步骤。
 * 【怎么做】注入 KafkaTemplate，send(topic, orderId, payload)。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   DB 提交 ──► KafkaTemplate.send
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.messaging;

import com.example.inventoryservice.event.InventoryReservationFailedEvent;
import com.example.inventoryservice.event.InventoryReservedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final KafkaTemplate<UUID, Object> kafkaTemplate;

    public InventoryEventPublisher(KafkaTemplate<UUID, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishReserved(InventoryReservedEvent event) {
        publish(KafkaTopics.INVENTORY_RESERVED, event.orderId(), event.eventId(), event);
    }

    public void publishReservationFailed(InventoryReservationFailedEvent event) {
        publish(KafkaTopics.INVENTORY_RESERVATION_FAILED, event.orderId(), event.eventId(), event);
    }

    private void publish(String topic, UUID orderId, UUID eventId, Object event) {
        log.info("Publishing inventory event: topic={}, eventId={}, orderId={}", topic, eventId, orderId);
        kafkaTemplate.send(topic, orderId, event);
    }
}
