/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryReservation.java                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】单笔订单的预占记录（orderId、商品、数量、状态）。
 * 【为什么】支付结果到达时需按 orderId 定位预占并确认或释放。
 * 【怎么做】UUID 主键 + ReservationStatus；提供 markConfirmed/markReleased 等领域方法。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   orderId ═══ 1 条 reservation 行
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "order_created_event_id", nullable = false)
    private UUID orderCreatedEventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryReservation() {
    }

    public InventoryReservation(
            UUID id,
            UUID orderId,
            String productName,
            int quantity,
            ReservationStatus status,
            UUID orderCreatedEventId,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.productName = productName;
        this.quantity = quantity;
        this.status = status;
        this.orderCreatedEventId = orderCreatedEventId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void markConfirmed(Instant updatedAt) {
        this.status = ReservationStatus.CONFIRMED;
        this.updatedAt = updatedAt;
    }

    public void markReleased(Instant updatedAt) {
        this.status = ReservationStatus.RELEASED;
        this.updatedAt = updatedAt;
    }
}
