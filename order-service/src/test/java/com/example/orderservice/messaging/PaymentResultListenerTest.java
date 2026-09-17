package com.example.orderservice.messaging;

import com.example.orderservice.event.PaymentCompletedEvent;
import com.example.orderservice.event.PaymentFailedEvent;
import com.example.orderservice.service.PaymentResultService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentResultListenerTest {

    private final PaymentResultService paymentResultService = mock(PaymentResultService.class);
    private final PaymentResultListener listener = new PaymentResultListener(paymentResultService);

    @Test
    void delegatesCompletedPaymentEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(), "PaymentCompleted", 1, Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("99.90")
        );

        listener.onPaymentCompleted(event);

        verify(paymentResultService).handleCompleted(event);
    }

    @Test
    void delegatesFailedPaymentEvent() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(), "PaymentFailed", 1, Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10000.01")
        );

        listener.onPaymentFailed(event);

        verify(paymentResultService).handleFailed(event);
    }
}
