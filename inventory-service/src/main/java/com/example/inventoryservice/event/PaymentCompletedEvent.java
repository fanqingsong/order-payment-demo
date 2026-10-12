/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentCompletedEvent.java                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】payment-completed 的入站 DTO（库存服务副本）。
 * 【为什么】支付成功后需「确认」预占，实扣库存。
 * 【怎么做】Java record，PaymentResultListener 消费。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   payment-completed ──► confirm 库存
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.event;

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
