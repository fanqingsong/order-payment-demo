/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · ProcessedEvent.java                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】以 InventoryReserved 的 eventId 幂等。
 * 【为什么】防止重复扣款。
 * 【怎么做】eventId PK + processedAt。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   dup eventId ──skip
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 已处理的 {@link com.example.paymentservice.event.InventoryReservedEvent#eventId()} 登记簿。
 * <p>
 * 主键即上游事件 ID：同一 OrderCreated 重投时 {@code existsById} 为 true，跳过重复支付逻辑。
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(UUID eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
