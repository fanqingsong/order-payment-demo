/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryItem.java                                         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】SKU 库存实体：可售 available 与已预占 reserved 数量。
 * 【为什么】库存是 Saga 中被修改的核心数据；预占/确认/释放都作用于此表。
 * 【怎么做】JPA @Entity；reserve()/confirmReservation()/releaseReservation() 封装数量变更规则。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   available ──reserve──► reserved
 *       ▲                  │
 *       └──release◄──confirm
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @Column(name = "product_name", nullable = false)
    private String productName;

    /**
     * 可售库存：尚未被订单占用、可继续预占的数量。
     * 与 reservedQuantity 的变换：
     *   reserve  → available -= qty，reserved += qty（可售转入预占）
     *   release  → reserved -= qty，available += qty（预占退回可售）
     *   confirm  → reserved -= qty（预占确认扣减，不再回到 available）
     */
    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    /**
     * 已预占库存：订单占用中、等待支付结果的数量。
     * 支付成功走 confirm（只减 reserved）；支付失败走 release（reserved 退回 available）。
     */
    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    protected InventoryItem() {
    }

    public InventoryItem(String productName, int availableQuantity, int reservedQuantity) {
        this.productName = productName;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public boolean canReserve(int quantity) {
        return availableQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient inventory for product: " + productName);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void confirmReservation(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot confirm reservation for product: " + productName);
        }
        reservedQuantity -= quantity;
    }

    public void releaseReservation(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release reservation for product: " + productName);
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }
}
