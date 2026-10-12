/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · Payment.java                                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】payment_db 中的支付记录 JPA 实体。
 * 【为什么】每笔订单扣款尝试一条记录。
 * 【怎么做】UUID id、orderId、amount、PaymentStatus、时间戳。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   Payment 行 / 订单
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 支付记录，映射 {@code payments} 表（独立库 payment_db）。
 * {@code orderId} 与订单服务的订单 ID 对应，跨服务仅通过 UUID 关联。
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(UUID orderId, BigDecimal amount, PaymentStatus status, Instant createdAt, Instant updatedAt) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
