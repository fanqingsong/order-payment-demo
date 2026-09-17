package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.event.OrderCreatedEvent;
import com.example.paymentservice.event.PaymentCompletedEvent;
import com.example.paymentservice.event.PaymentFailedEvent;
import com.example.paymentservice.messaging.PaymentEventPublisher;
import com.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @Test
    void completesPaymentAtTheLimitAndPublishesCompletedEvent() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentService paymentService = paymentServiceWithGeneratedId(paymentId);
        OrderCreatedEvent orderEvent = orderEvent(orderId, "10000.00");

        paymentService.processOrder(orderEvent);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(eventPublisher).publishCompleted(eventCaptor.capture());
        PaymentCompletedEvent event = eventCaptor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo("PaymentCompleted");
        assertThat(event.eventVersion()).isEqualTo(1);
        assertThat(event.paymentId()).isEqualTo(paymentId);
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.amount()).isEqualByComparingTo("10000.00");
        verify(eventPublisher, never()).publishFailed(any());
    }

    @Test
    void failsPaymentAboveTheLimitAndPublishesFailedEvent() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentService paymentService = paymentServiceWithGeneratedId(paymentId);
        OrderCreatedEvent orderEvent = orderEvent(orderId, "10000.01");

        paymentService.processOrder(orderEvent);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);

        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(eventPublisher).publishFailed(eventCaptor.capture());
        PaymentFailedEvent event = eventCaptor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo("PaymentFailed");
        assertThat(event.eventVersion()).isEqualTo(1);
        assertThat(event.paymentId()).isEqualTo(paymentId);
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.amount()).isEqualByComparingTo("10000.01");
        verify(eventPublisher, never()).publishCompleted(any());
    }

    private PaymentService paymentServiceWithGeneratedId(UUID paymentId) {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", paymentId);
            return payment;
        });
        return new PaymentService(paymentRepository, eventPublisher);
    }

    private OrderCreatedEvent orderEvent(UUID orderId, String amount) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                "OrderCreated",
                1,
                Instant.now(),
                orderId,
                UUID.randomUUID(),
                "Keyboard",
                new BigDecimal(amount)
        );
    }
}
