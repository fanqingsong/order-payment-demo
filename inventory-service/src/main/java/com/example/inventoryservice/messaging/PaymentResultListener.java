/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentResultListener.java                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订阅 payment-completed / payment-failed 的监听器。
 * 【为什么】支付终态后库存侧最终一致：确认扣减或释放。
 * 【怎么做】两个 @KafkaListener 方法分别调用 confirmAfterPayment / releaseAfterPaymentFailure。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   payment-* ──► confirm | release
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.messaging;

import com.example.inventoryservice.event.PaymentCompletedEvent;
import com.example.inventoryservice.event.PaymentFailedEvent;
import com.example.inventoryservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final InventoryService inventoryService;

    public PaymentResultListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_COMPLETED,
            properties = {
                    "spring.json.value.default.type=com.example.inventoryservice.event.PaymentCompletedEvent"
            }
    )
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: eventId={}, orderId={}", event.eventId(), event.orderId());
        inventoryService.confirmAfterPayment(event);
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            properties = {
                    "spring.json.value.default.type=com.example.inventoryservice.event.PaymentFailedEvent"
            }
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: eventId={}, orderId={}", event.eventId(), event.orderId());
        inventoryService.releaseAfterPaymentFailure(event);
    }
}
