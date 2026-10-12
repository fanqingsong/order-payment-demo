/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderCreatedEvent.java                                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】下单后写入 Outbox/Kafka 的 OrderCreated 载荷。
 * 【为什么】启动 inventory 预占链路。
 * 【怎么做】record：eventId、orderId、userId、product、quantity、amount。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   createOrder ──► OrderCreated ──► order-created
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 领域事件：订单已成功持久化，下游应发起支付等流程。
 * <p>
 * 使用 Java {@code record} 便于 JSON 序列化/不可变。
 * {@code eventId}：全局唯一，支付侧用于幂等；{@code eventVersion}：预留 schema 演进。
 */
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
