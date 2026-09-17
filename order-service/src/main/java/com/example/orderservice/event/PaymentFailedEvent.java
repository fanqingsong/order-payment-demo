package com.example.orderservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID paymentId,
        UUID orderId,
        BigDecimal amount
) {
}
