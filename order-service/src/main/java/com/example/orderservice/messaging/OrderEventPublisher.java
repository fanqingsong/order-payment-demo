package com.example.orderservice.messaging;

import com.example.orderservice.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 将 {@link OrderCreatedEvent} 写入 Kafka 主题 {@link KafkaTopics#ORDER_CREATED}。
 * <p>
 * 消息 key 使用 {@code orderId}：同一订单的相关消息会落在同一分区，便于保证顺序（单分区内）。
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<UUID, OrderCreatedEvent> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<UUID, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 异步发送；返回 {@link CompletableFuture} 供 OutboxPublisher 等待确认。
     */
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
