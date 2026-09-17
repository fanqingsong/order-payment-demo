package com.example.paymentservice.messaging;

import com.example.paymentservice.event.PaymentCompletedEvent;
import com.example.paymentservice.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
