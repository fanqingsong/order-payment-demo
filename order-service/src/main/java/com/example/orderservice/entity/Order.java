/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · Order.java                                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订单 JPA 实体（order_db.orders）。
 * 【为什么】订单状态机在本服务落库，Saga 异步步回写此表。
 * 【怎么做】applyInventoryReserved/Failed、updatePaymentStatus；初始 PENDING_INVENTORY。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   PENDING_INVENTORY ──► PENDING_PAYMENT ──► PAID
 *          └── INVENTORY_FAILED / PAYMENT_FAILED
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.entity;

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
 * 订单 JPA 实体，映射表 {@code orders}。
 * <p>
 * 创建后状态为 {@link OrderStatus#PENDING_INVENTORY}，经库存预占后再进入待支付。
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 下单用户，与 {@code users.id} 逻辑关联（本 demo 未建外键，便于服务边界清晰）。 */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA 要求无参构造；业务代码请用有参构造。 */
    protected Order() {
    }

    public Order(UUID userId, String productName, Integer quantity, BigDecimal unitPrice,
                 BigDecimal totalAmount, OrderStatus status, Instant createdAt, Instant updatedAt) {
        this.userId = userId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 仅允许从「待支付」变为已支付/支付失败；非法转换在实体层抛异常，防止业务 bug 写坏数据。
     */
    public void updatePaymentStatus(OrderStatus status, Instant updatedAt) {
        if (this.status != OrderStatus.PENDING_PAYMENT && this.status != OrderStatus.PENDING_INVENTORY) {
            throw new IllegalStateException("Only pending orders can receive a payment result");
        }
        if (status != OrderStatus.PAID && status != OrderStatus.PAYMENT_FAILED) {
            throw new IllegalArgumentException("Payment result must be PAID or PAYMENT_FAILED");
        }
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public void applyInventoryReserved(Instant updatedAt) {
        if (this.status != OrderStatus.PENDING_INVENTORY) {
            throw new IllegalStateException("Only pending-inventory orders can receive inventory reserved");
        }
        this.status = OrderStatus.PENDING_PAYMENT;
        this.updatedAt = updatedAt;
    }

    public void applyInventoryFailed(Instant updatedAt) {
        if (this.status != OrderStatus.PENDING_INVENTORY) {
            throw new IllegalStateException("Only pending-inventory orders can receive inventory failure");
        }
        this.status = OrderStatus.INVENTORY_FAILED;
        this.updatedAt = updatedAt;
    }
}
