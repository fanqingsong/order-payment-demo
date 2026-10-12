/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentCompletedEvent.java                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】payment-completed JSON 的本地 record DTO。
 * 【为什么】与 payment-service 字段对齐，独立模块编译。
 * 【怎么做】Java record，Listener 反序列化用。
 *
 * 示意图（notification-service · 8082 · 无库）：
 *   JSON ──► PaymentCompletedEvent
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.notificationservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 与 payment-service 发布的 JSON 结构一致的本地类型定义。 */
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
