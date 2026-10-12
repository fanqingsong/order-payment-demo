/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OutboxStatus.java                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】Outbox 行状态：PENDING / PUBLISHED。
 * 【为什么】OutboxPublisher 只扫描 PENDING。
 * 【怎么做】枚举字段 on OutboxEvent.status。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   PENDING ──发送成功──► PUBLISHED
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.entity;

/** Outbox 行是否已成功投递到 Kafka。 */
public enum OutboxStatus {
    /** 等待 OutboxPublisher 发送。 */
    PENDING,
    /** 已发送并确认（本 demo 在 send 成功后标记）。 */
    PUBLISHED
}
