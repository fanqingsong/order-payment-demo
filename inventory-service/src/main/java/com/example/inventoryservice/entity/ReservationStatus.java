/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · ReservationStatus.java                                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】预占状态枚举：RESERVED / CONFIRMED / RELEASED。
 * 【为什么】约束非法状态迁移（如对 RELEASED 再确认）。
 * 【怎么做】枚举存为字符串列，绑定在 InventoryReservation.status。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   RESERVED ──付成──► CONFIRMED
 *       └──付败──► RELEASED
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.entity;

public enum ReservationStatus {
    RESERVED,
    CONFIRMED,
    RELEASED
}
