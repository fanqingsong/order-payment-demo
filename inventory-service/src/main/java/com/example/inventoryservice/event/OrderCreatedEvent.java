/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderCreatedEvent.java                                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】order-created 消息在本服务内的反序列化 DTO。
 * 【为什么】各微服务独立编译，JSON 字段一致即可互通。
 * 【怎么做】Java record，OrderCreatedListener 从 Kafka 反序列化后传入 InventoryService。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   order-created(JSON) ──► OrderCreatedEvent
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        String productName,
        int quantity,
        BigDecimal amount
) {
}
