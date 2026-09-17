package com.example.notificationservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID paymentId,
        UUID orderId,
        BigDecimal amount
) {
}
