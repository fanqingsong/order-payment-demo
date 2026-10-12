/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentServiceTest.java                                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】PaymentService 边界金额与幂等单元测试。
 * 【为什么】10000.00 OK / 10000.01 FAIL 等。
 * 【怎么做】Mock repos/publisher + captors。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   10000.00 OK │ 10000.01 FAIL
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.entity.ProcessedEvent;
import com.example.paymentservice.event.InventoryReservedEvent;
import com.example.paymentservice.event.PaymentCompletedEvent;
import com.example.paymentservice.event.PaymentFailedEvent;
import com.example.paymentservice.messaging.PaymentEventPublisher;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.repository.ProcessedEventRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @Test
    void completesPaymentAtTheLimitAndPublishesCompletedEvent() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentService paymentService = paymentServiceWithGeneratedId(paymentId);
        InventoryReservedEvent inventoryEvent = inventoryEvent(orderId, "10000.00");

        paymentService.processInventoryReserved(inventoryEvent);

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
        InventoryReservedEvent inventoryEvent = inventoryEvent(orderId, "10000.01");

        paymentService.processInventoryReserved(inventoryEvent);

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

    @Test
    void createsOnlyOnePaymentWhenTheSameEventIsDeliveredTwice() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentService paymentService = paymentServiceWithGeneratedId(paymentId);
        InventoryReservedEvent inventoryEvent = inventoryEvent(orderId, "99.99");
        when(processedEventRepository.existsById(inventoryEvent.eventId())).thenReturn(false, true);

        paymentService.processInventoryReserved(inventoryEvent);
        paymentService.processInventoryReserved(inventoryEvent);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        verify(eventPublisher, times(1)).publishCompleted(any(PaymentCompletedEvent.class));
        verify(eventPublisher, never()).publishFailed(any());
    }

    private PaymentService paymentServiceWithGeneratedId(UUID paymentId) {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", paymentId);
            return payment;
        });
        return new PaymentService(paymentRepository, processedEventRepository, eventPublisher);
    }

    private InventoryReservedEvent inventoryEvent(UUID orderId, String amount) {
        return new InventoryReservedEvent(
                UUID.randomUUID(),
                "InventoryReserved",
                1,
                Instant.now(),
                orderId,
                UUID.randomUUID(),
                "Keyboard",
                1,
                new BigDecimal(amount),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}
