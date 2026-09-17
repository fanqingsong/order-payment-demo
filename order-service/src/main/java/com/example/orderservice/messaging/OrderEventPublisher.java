package com.example.orderservice.messaging;

import com.example.orderservice.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<UUID, OrderCreatedEvent> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<UUID, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<UUID, OrderCreatedEvent>> publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent: eventId={}, orderId={}", event.eventId(), event.orderId());
        CompletableFuture<SendResult<UUID, OrderCreatedEvent>> future =
                kafkaTemplate.send(KafkaTopics.ORDER_CREATED, event.orderId(), event);
        future
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        log.info("Published OrderCreatedEvent: eventId={}, orderId={}",
                                event.eventId(), event.orderId());
                    } else {
                        log.error("Failed to publish OrderCreatedEvent: eventId={}, orderId={}",
                                event.eventId(), event.orderId(), exception);
                    }
                });
        return future;
    }
}
