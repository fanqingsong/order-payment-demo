/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentFailedEvent.java                                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】payment-failed JSON 的本地 record DTO。
 * 【为什么】同上，跨服务靠 JSON 契约。
 * 【怎么做】Java record。
 *
 * 示意图（notification-service · 8082 · 无库）：
 *   JSON ──► PaymentFailedEvent
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.notificationservice.event;

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
