/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · ProcessedEventRepository.java                              ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】processed_events 幂等表的 JPA 访问。
 * 【为什么】支撑 existsById 去重。
 * 【怎么做】JpaRepository<ProcessedEvent, UUID>。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   eventId ──► 账本
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
