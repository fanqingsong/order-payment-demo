/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryReservedEvent.java                                ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】预占成功时发往 Kafka 的事件 record。
 * 【为什么】同时触发 payment-service 与 order-service 的下一步。
 * 【怎么做】含 amount、reservationId、orderCreatedEventId 等追踪字段。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   预占 OK ──► inventory-reserved
 *        ├────► Payment
 *        └────► Order
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.event;

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
