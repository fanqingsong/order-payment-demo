/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentEventPublisherTest.java                             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】PaymentEventPublisher 单元测试。
 * 【为什么】publishCompleted 使用 orderId 作 key。
 * 【怎么做】Mock KafkaTemplate verify。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   publishCompleted ──► send(key)
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
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
