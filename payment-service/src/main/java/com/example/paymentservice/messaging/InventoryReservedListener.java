/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryReservedListener.java                             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订阅 inventory-reserved 的监听器。
 * 【为什么】Saga 支付步入口。
 * 【怎么做】PaymentService.processInventoryReserved；KAFKA_FAIL 模拟失败。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   inventory-reserved ──► PaymentService
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.messaging;

import com.example.paymentservice.event.InventoryReservedEvent;
import com.example.paymentservice.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryReservedListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservedListener.class);

    private final PaymentService paymentService;

    public InventoryReservedListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_RESERVED)
    public void onInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent: eventId={}, orderId={}", event.eventId(), event.orderId());

        if ("KAFKA_FAIL".equals(event.productName())) {
            log.warn("Intentionally failing InventoryReservedEvent: eventId={}, orderId={}",
                    event.eventId(), event.orderId());
            throw new IllegalStateException("Simulated Kafka consumer failure");
        }
        paymentService.processInventoryReserved(event);
    }
}
