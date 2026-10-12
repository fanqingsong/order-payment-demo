/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentFailedEvent.java                                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】支付失败出站 Kafka DTO。
 * 【为什么】触发订单失败与库存释放。
 * 【怎么做】PaymentEventPublisher.publishFailed。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   pay FAIL ──► payment-failed
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 支付失败后发往 Kafka 的消息体结构。 */
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
