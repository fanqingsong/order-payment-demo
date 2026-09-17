package com.example.orderservice.messaging;

import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.entity.OutboxStatus;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxPublisher outboxPublisher;
    private OrderCreatedEvent event;
    private OutboxEvent outboxEvent;

    @BeforeEach
    void setUp() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        event = new OrderCreatedEvent(
                eventId,
                "OrderCreated",
                1,
                Instant.now(),
                orderId,
                UUID.randomUUID(),
                "Keyboard",
                new BigDecimal("99.90")
        );
        outboxEvent = new OutboxEvent(eventId, orderId, "OrderCreated", "{}", Instant.now());
        outboxPublisher = new OutboxPublisher(outboxEventRepository, orderEventPublisher, objectMapper);

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(outboxEvent));
        when(objectMapper.readValue("{}", OrderCreatedEvent.class)).thenReturn(event);
    }

    @Test
    void failedPublishingLeavesEventAvailableForRetry() {
        CompletableFuture<SendResult<UUID, OrderCreatedEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(orderEventPublisher.publishOrderCreated(event)).thenReturn(failed);

        outboxPublisher.publishPendingEvents();

        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxEvent.getPublishedAt()).isNull();
        verify(orderEventPublisher).publishOrderCreated(event);
    }

    @Test
    void successfulPublishingMarksEventPublished() {
        CompletableFuture<SendResult<UUID, OrderCreatedEvent>> succeeded =
                CompletableFuture.completedFuture(null);
        when(orderEventPublisher.publishOrderCreated(event)).thenReturn(succeeded);

        outboxPublisher.publishPendingEvents();

        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outboxEvent.getPublishedAt()).isNotNull();
        verify(orderEventPublisher).publishOrderCreated(event);
    }
}
