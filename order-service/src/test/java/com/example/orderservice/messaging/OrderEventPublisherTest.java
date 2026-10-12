/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderEventPublisherTest.java                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】OrderEventPublisher 单元测试。
 * 【为什么】断言 Kafka message key 为 orderId。
 * 【怎么做】Mock KafkaTemplate + verify send。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   publish ──verify──► send(key=orderId)
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.messaging;

import com.example.orderservice.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private KafkaTemplate<UUID, OrderCreatedEvent> kafkaTemplate;

    @Test
    void publishesOrderCreatedEventUsingOrderIdAsKey() {
        OrderEventPublisher publisher = new OrderEventPublisher(kafkaTemplate);
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "OrderCreated",
                1,
                Instant.now(),
                orderId,
                UUID.randomUUID(),
                "Keyboard",
                1,
                new BigDecimal("99.90")
        );
        CompletableFuture<SendResult<UUID, OrderCreatedEvent>> future =
                CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(KafkaTopics.ORDER_CREATED, orderId, event)).thenReturn(future);

        CompletableFuture<SendResult<UUID, OrderCreatedEvent>> result = publisher.publishOrderCreated(event);

        verify(kafkaTemplate).send(KafkaTopics.ORDER_CREATED, orderId, event);
        org.assertj.core.api.Assertions.assertThat(result).isSameAs(future);
    }
}
