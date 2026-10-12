/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentStatus.java                                         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】支付状态 PROCESSING / COMPLETED / FAILED。
 * 【为什么】Demo 立即根据金额规则设终态。
 * 【怎么做】字符串枚举列。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   金额规则 ──► COMPLETED|FAILED
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.entity;

/**
 * 支付单状态。本 demo 创建时直接设为 COMPLETED 或 FAILED；
 * {@link #PROCESSING} 预留给异步网关回调等更复杂场景。
 */
public enum PaymentStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
