/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentEventPublisher.java                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】发布 payment-completed / payment-failed。
 * 【为什么】多 consumer group 并行订阅。
 * 【怎么做】KafkaTemplate.send(orderId key) + 日志。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   PaymentService ──► payment-*
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.messaging;

import com.example.paymentservice.event.PaymentCompletedEvent;
import com.example.paymentservice.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 支付结果事件的生产者：通知订单服务与通知服务更新状态 / 发邮件。
 * <p>
 * 使用 {@code orderId} 作为消息 key，与订单侧、通知侧消费者分区策略一致。
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<UUID, Object> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<UUID, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCompleted(PaymentCompletedEvent event) {
        publish(KafkaTopics.PAYMENT_COMPLETED, event.orderId(), event.eventId(), event.paymentId(), event);
    }

    public void publishFailed(PaymentFailedEvent event) {
        publish(KafkaTopics.PAYMENT_FAILED, event.orderId(), event.eventId(), event.paymentId(), event);
    }

    private void publish(String topic, UUID orderId, UUID eventId, UUID paymentId, Object event) {
        log.info("Publishing payment event: eventId={}, orderId={}, paymentId={}", eventId, orderId, paymentId);
        kafkaTemplate.send(topic, orderId, event).whenComplete((result, exception) -> {
            if (exception == null) {
                log.info("Published payment event: eventId={}, orderId={}, paymentId={}",
                        eventId, orderId, paymentId);
            } else {
                log.error("Failed to publish payment event: eventId={}, orderId={}, paymentId={}",
                        eventId, orderId, paymentId, exception);
            }
        });
    }
}
