package com.example.orderservice.messaging;

import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.entity.OutboxStatus;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OrderEventPublisher orderEventPublisher,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
                .forEach(this::publish);
    }

    private void publish(OutboxEvent outboxEvent) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(
                    outboxEvent.getPayload(),
                    OrderCreatedEvent.class
            );
            orderEventPublisher.publishOrderCreated(event).get();
            outboxEvent.markPublished(Instant.now());
            log.info("Published outbox event: eventId={}, orderId={}",
                    outboxEvent.getId(), outboxEvent.getAggregateId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Outbox publishing interrupted; event remains pending: eventId={}, orderId={}",
                    outboxEvent.getId(), outboxEvent.getAggregateId(), exception);
        } catch (Exception exception) {
            log.warn("Failed to publish outbox event; event remains pending: eventId={}, orderId={}",
                    outboxEvent.getId(), outboxEvent.getAggregateId(), exception);
        }
    }
}
