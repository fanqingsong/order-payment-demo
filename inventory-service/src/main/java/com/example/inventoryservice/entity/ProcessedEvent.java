/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · ProcessedEvent.java                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】已处理 eventId 的幂等账本实体。
 * 【为什么】Kafka 至少一次投递，重复消息不能重复扣库存。
 * 【怎么做】eventId 主键 + processedAt；处理前 existsById，成功后同事务 insert。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   eventId 已存在? ──是──► 跳过
 *         └──否──► 业务 + 记账
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(UUID eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }
}
