/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentResultListener.java                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】支付结果的 Kafka 监听器。
 * 【为什么】与 Order/Inventory 并行消费（不同 consumer group）。
 * 【怎么做】@KafkaListener 转调 NotificationService。
 *
 * 示意图（notification-service · 8082 · 无库）：
 *   payment-* ──► NotificationService
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.notificationservice.messaging;

import com.example.notificationservice.event.PaymentCompletedEvent;
import com.example.notificationservice.event.PaymentFailedEvent;
import com.example.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 与订单服务并行订阅支付结果主题，实现<strong>读模型分离</strong>：
 * 订单服务改库，通知服务发信，互不阻塞。
 * <p>
 * 事件类定义在 notification-service 包内（与 order/payment 侧结构相同），
 * 因为每个微服务独立编译，JSON 字段一致即可互通。
 */
@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final NotificationService notificationService;

    public PaymentResultListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_COMPLETED,
            properties = "spring.json.value.default.type=com.example.notificationservice.event.PaymentCompletedEvent"
    )
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: eventId={}, orderId={}, paymentId={}",
                event.eventId(), event.orderId(), event.paymentId());
        notificationService.sendPaymentSuccessful(event.orderId());
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            properties = "spring.json.value.default.type=com.example.notificationservice.event.PaymentFailedEvent"
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: eventId={}, orderId={}, paymentId={}",
                event.eventId(), event.orderId(), event.paymentId());
        notificationService.sendPaymentFailed(event.orderId());
    }
}
