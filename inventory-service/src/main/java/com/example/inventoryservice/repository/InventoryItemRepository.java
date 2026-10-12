/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryItemRepository.java                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】InventoryItem 的 Spring Data JPA Repository。
 * 【为什么】持久化 SKU 库存行。
 * 【怎么做】extends JpaRepository<InventoryItem, String>，主键为 productName。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   Service ──► Repo ──► inventory_items 表
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
}
