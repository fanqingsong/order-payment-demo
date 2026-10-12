/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentFailedEvent.java                                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】消费 payment-failed 的 DTO。
 * 【为什么】订单置 PAYMENT_FAILED。
 * 【怎么做】PaymentResultListener → PaymentResultService。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   payment-failed ──► PAYMENT_FAILED
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 支付服务发布的「支付失败」事件。 */
public record PaymentFailedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID paymentId,
        UUID orderId,
        BigDecimal amount
) {
}
