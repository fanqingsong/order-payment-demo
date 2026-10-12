/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OutboxEvent.java                                           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】事务性 Outbox 表行：待发 Kafka 的 JSON 载荷。
 * 【为什么】下单与发 order-created 解耦，保证库与消息最终一致。
 * 【怎么做】含 eventType、payload、OutboxStatus；markPublished() 更新状态。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   同事务: Order + Outbox(PENDING)
 *        └──► OutboxPublisher ──► PUBLISHED
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 事务性发件箱表 {@code outbox_events} 的一行记录。
 * <p>
 * {@code id} 与领域事件的 {@code eventId} 相同，便于追踪。
 * {@code aggregateId} 在本项目中即 {@code orderId}。
 * {@code payload} 为 JSON 字符串，由 {@link com.example.orderservice.messaging.OutboxPublisher} 反序列化后发送 Kafka。
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID id, UUID aggregateId, String eventType, String payload, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
        this.status = OutboxStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    /** Kafka 发送成功后调用，标记为已发布并记录时间。 */
    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.status = OutboxStatus.PUBLISHED;
    }
}
