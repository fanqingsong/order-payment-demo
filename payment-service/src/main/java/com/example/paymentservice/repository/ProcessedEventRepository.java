/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · ProcessedEventRepository.java                              ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】processed_events JPA。
 * 【为什么】existsById 幂等。
 * 【怎么做】JpaRepository<ProcessedEvent, UUID>。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   eventId 账本
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.repository;

import com.example.paymentservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 主键类型为 {@link ProcessedEvent#getEventId()}（即上游 Kafka 事件的 eventId）。 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
