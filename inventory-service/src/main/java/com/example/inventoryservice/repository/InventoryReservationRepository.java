/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryReservationRepository.java                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】InventoryReservation 的 JPA Repository。
 * 【为什么】按 orderId 查询预占记录。
 * 【怎么做】findByOrderId(UUID) + 标准 save/findById。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   orderId ──► Optional<Reservation>
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    Optional<InventoryReservation> findByOrderId(UUID orderId);
}
