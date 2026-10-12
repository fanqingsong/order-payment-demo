/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryReservationFailedEvent.java                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】预占失败时发往 Kafka 的事件 record。
 * 【为什么】通知 order-service 置 INVENTORY_FAILED，支付链路不会启动。
 * 【怎么做】含 orderId、product、quantity、reason；InventoryEventPublisher 发送。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   缺货 ──► inventory-reservation-failed ──► Order
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.event;

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
