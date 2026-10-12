/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentCompletedEvent.java                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】支付成功出站 Kafka DTO。
 * 【为什么】广播给 order、inventory、notification。
 * 【怎么做】PaymentEventPublisher.publishCompleted。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   pay OK ──► payment-completed
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 支付成功后发往 Kafka 的消息体结构。 */
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
