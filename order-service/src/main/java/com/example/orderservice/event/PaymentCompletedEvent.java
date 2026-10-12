/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentCompletedEvent.java                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】消费 payment-completed 的 DTO。
 * 【为什么】订单置 PAID。
 * 【怎么做】PaymentResultListener → PaymentResultService。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   payment-completed ──► PAID
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 支付服务发布的「支付成功」事件，订单服务据此将状态改为 PAID。 */
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
