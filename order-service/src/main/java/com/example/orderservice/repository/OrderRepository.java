/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderRepository.java                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】Order 实体的 Spring Data JPA。
 * 【为什么】用户维度列表与按 id+userId 查询。
 * 【怎么做】findAllByUserIdOrderByCreatedAtDesc、findByIdAndUserId。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   OrderService ──► orders 表
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 订单数据访问。方法名遵循 Spring Data JPA 派生查询约定（无需手写 SQL）。
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);
}
