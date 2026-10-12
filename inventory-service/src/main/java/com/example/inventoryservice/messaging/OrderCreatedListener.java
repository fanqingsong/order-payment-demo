/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderCreatedListener.java                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订阅 order-created 的 Kafka 监听器。
 * 【为什么】订单创建后自动进入「预占库存」Saga 步。
 * 【怎么做】@KafkaListener → InventoryService.reserveForOrder；商品 KAFKA_FAIL 可模拟失败。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   order-created ──► reserveForOrder()
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.messaging;

import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final InventoryService inventoryService;

    public OrderCreatedListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_CREATED,
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    /**
     * 若本方法执行到一半进程崩溃/未正常返回，Kafka 通常会再次投递同一条消息。
     *
     * 机制（at-least-once）：
     * 1. Spring Kafka 默认在监听方法成功返回后才提交 offset（ack）。
     * 2. 中途断点、抛异常（如下方 KAFKA_FAIL 模拟）或容器重启时，offset 未提交，
     *    broker 仍认为该消息未消费，会按 consumer group 重新投递。
     * 3. 因此下游必须幂等：InventoryService.reserveForOrder 用 ProcessedEvent
     *    按 eventId 去重，避免重复预占。
     *
     * 注意：若事务已提交、offset 也已提交后才崩溃，则不会重投；
     * 若事务已提交但 publish/ack 前失败，则可能重投（需幂等或 Outbox）。
     */
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: eventId={}, orderId={}", event.eventId(), event.orderId());

        if ("KAFKA_FAIL".equals(event.productName())) {
            log.warn("Intentionally failing OrderCreatedEvent: eventId={}, orderId={}",
                    event.eventId(), event.orderId());
            // 抛异常 → 不提交 offset → Kafka 会再次投递，用于演示重试
            throw new IllegalStateException("Simulated Kafka consumer failure");
        }
        inventoryService.reserveForOrder(event);
    }
}
