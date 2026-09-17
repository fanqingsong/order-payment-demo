package com.example.paymentservice.messaging;

import com.example.paymentservice.event.OrderCreatedEvent;
import com.example.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderCreatedListenerTest {

    @Test
    void delegatesConsumedEventToPaymentService() {
        PaymentService paymentService = mock(PaymentService.class);
        OrderCreatedListener listener = new OrderCreatedListener(paymentService);
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "OrderCreated",
                1,
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("99.99")
        );

        listener.onOrderCreated(event);

        verify(paymentService).processOrder(event);
    }
}
