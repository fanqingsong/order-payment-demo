package com.example.paymentservice.messaging;

import com.example.paymentservice.event.PaymentCompletedEvent;
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
class PaymentEventPublisherTest {

    @Mock
    private KafkaTemplate<UUID, Object> kafkaTemplate;

    @Test
    void publishesCompletedEventUsingOrderIdAsKey() {
        PaymentEventPublisher publisher = new PaymentEventPublisher(kafkaTemplate);
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                "PaymentCompleted",
                1,
                Instant.now(),
                UUID.randomUUID(),
                orderId,
                new BigDecimal("125.00")
        );
        CompletableFuture<SendResult<UUID, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(KafkaTopics.PAYMENT_COMPLETED, orderId, event)).thenReturn(future);

        publisher.publishCompleted(event);

        verify(kafkaTemplate).send(KafkaTopics.PAYMENT_COMPLETED, orderId, event);
    }
}
