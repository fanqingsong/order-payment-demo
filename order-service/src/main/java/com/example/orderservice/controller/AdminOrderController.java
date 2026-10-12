/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · AdminOrderController.java                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】管理员 REST：GET /api/admin/orders 列出全部订单。
 * 【为什么】运营视角与用户 API 分离。
 * 【怎么做】@RestController + OrderService.getAllOrders()；需 ADMIN 角色。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   ADMIN + JWT ──► 全量订单列表
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员订单接口。访问控制：{@code SecurityConfig} 要求 {@code ROLE_ADMIN}。
 * <p>
 * 与 {@link OrderController} 分离，便于清晰表达「运营后台」与「用户自助」两种 API 面。
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getOrders() {
        return orderService.getAllOrders();
    }
}
