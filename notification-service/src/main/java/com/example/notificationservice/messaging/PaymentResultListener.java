package com.example.notificationservice.messaging;

import com.example.notificationservice.event.PaymentCompletedEvent;
import com.example.notificationservice.event.PaymentFailedEvent;
import com.example.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
