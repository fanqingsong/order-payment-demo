/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OutboxEventRepository.java                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】Outbox 表访问；轮询待发送记录。
 * 【为什么】多实例下 pessimistic lock 降低重复发送风险。
 * 【怎么做】findTop100ByStatusOrderByCreatedAtAsc + @Lock PESSIMISTIC_WRITE。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   PENDING 行 ──FOR UPDATE──► 发送
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.repository;

import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.entity.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.UUID;

/**
 * Outbox 表访问。查询待发送记录时使用悲观写锁，降低多实例同时发布同一行的概率（本地 demo 通常单实例）。
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
