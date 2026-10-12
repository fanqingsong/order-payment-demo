/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderStatus.java                                           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订单生命周期枚举。
 * 【为什么】与 inventory/payment 事件对齐，支持乱序支付结果。
 * 【怎么做】PENDING_INVENTORY、PENDING_PAYMENT、INVENTORY_FAILED、PAID、PAYMENT_FAILED。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   见 docs/architecture.md 状态图
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.entity;

/**
 * 订单生命周期状态（简化版）。
 * <ul>
 *   <li>{@link #PENDING_INVENTORY} — 已创建，等待库存预占</li>
 *   <li>{@link #PENDING_PAYMENT} — 库存已预占，等待支付</li>
 *   <li>{@link #INVENTORY_FAILED} — 库存不足等导致无法继续</li>
 *   <li>{@link #PAID} — 支付成功</li>
 *   <li>{@link #PAYMENT_FAILED} — 支付失败（如超过演示限额）</li>
 * </ul>
 */
public enum OrderStatus {
    PENDING_INVENTORY,
    PENDING_PAYMENT,
    INVENTORY_FAILED,
    PAID,
    PAYMENT_FAILED
}
