/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryReservationFailedEvent.java                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】消费 inventory-reservation-failed 的 DTO。
 * 【为什么】库存失败时更新订单状态。
 * 【怎么做】Java record → InventoryResultService。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   inv-fail event ──► INVENTORY_FAILED
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservationFailedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID orderId,
        String productName,
        int quantity,
        String reason
) {
}
