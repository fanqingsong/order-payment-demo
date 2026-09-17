package com.example.orderservice.messaging;

import com.example.orderservice.event.PaymentCompletedEvent;
import com.example.orderservice.event.PaymentFailedEvent;
import com.example.orderservice.service.PaymentResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final PaymentResultService paymentResultService;

    public PaymentResultListener(PaymentResultService paymentResultService) {
        this.paymentResultService = paymentResultService;
    }

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
