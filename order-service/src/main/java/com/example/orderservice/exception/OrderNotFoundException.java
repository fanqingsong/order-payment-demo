/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderNotFoundException.java                                ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订单不存在或无权访问异常。
 * 【为什么】返回 404，避免泄露资源存在性。
 * 【怎么做】携带 orderId 构造消息。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   无订单 ──► 404
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.exception;

import java.util.UUID;

/** 订单不存在或用户无权查看时抛出，映射 HTTP 404。 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("Order not found: " + id);
    }
}
