/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryReservedEvent.java                                ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】inventory-reserved 入站 DTO。
 * 【为什么】支付只在预占成功后启动（不消费 order-created）。
 * 【怎么做】Java record → PaymentService.processInventoryReserved。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   inventory-reserved ──► pay
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.event;

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
