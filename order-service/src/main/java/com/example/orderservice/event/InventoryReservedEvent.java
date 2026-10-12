/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryReservedEvent.java                                ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】消费 inventory-reserved 的 DTO。
 * 【为什么】库存成功 → PENDING_PAYMENT。
 * 【怎么做】Java record → InventoryResultService。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   inv-OK event ──► PENDING_PAYMENT
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        String productName,
        int quantity,
        BigDecimal amount,
        UUID orderCreatedEventId,
        UUID reservationId
) {
}
