/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentResultListener.java                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】消费 payment-completed / payment-failed。
 * 【为什么】异步更新订单支付终态。
 * 【怎么做】PaymentResultService.handleCompleted/Failed。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   payment-* ──► PaymentResultService
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.messaging;

import com.example.orderservice.event.PaymentCompletedEvent;
import com.example.orderservice.event.PaymentFailedEvent;
import com.example.orderservice.service.PaymentResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 消费支付服务发布的<strong>支付结果</strong>，驱动订单状态从 {@code PENDING_PAYMENT} 变更。
 * <p>
 * 与 {@link com.example.paymentservice.messaging.PaymentEventPublisher} 成对：
 * payment-service 生产 → order-service 消费 → {@link PaymentResultService} 更新 {@code orders} 表。
 */
@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final PaymentResultService paymentResultService;

    public PaymentResultListener(PaymentResultService paymentResultService) {
        this.paymentResultService = paymentResultService;
    }

    /**
     * {@code spring.json.value.default.type}：反序列化时没有类型头信息，需指定 JSON → Java 类型。
     */
    @KafkaListener(
            topics = KafkaTopics.PAYMENT_COMPLETED,
            properties = "spring.json.value.default.type=com.example.orderservice.event.PaymentCompletedEvent"
    )
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: eventId={}, orderId={}, paymentId={}",
                event.eventId(), event.orderId(), event.paymentId());
        paymentResultService.handleCompleted(event);
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            properties = "spring.json.value.default.type=com.example.orderservice.event.PaymentFailedEvent"
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: eventId={}, orderId={}, paymentId={}",
                event.eventId(), event.orderId(), event.paymentId());
        paymentResultService.handleFailed(event);
    }
}
