/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderResponse.java                                         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订单 API 响应投影 record。
 * 【为什么】隐藏 JPA 实体细节，稳定对外契约。
 * 【怎么做】OrderService 从 Order 实体映射字段。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   Order(entity) ──map──► OrderResponse
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.dto;

import com.example.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 订单 API 响应，面向前端/调用方，与 {@link com.example.orderservice.entity.Order} 字段对应。 */
public record OrderResponse(
        UUID id,
        UUID userId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
