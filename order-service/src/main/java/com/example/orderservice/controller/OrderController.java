/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderController.java                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】用户订单 REST：创建、列表、按 ID 查询。
 * 【为什么】从 JWT 取 userId，防止 body 伪造身份。
 * 【怎么做】@AuthenticationPrincipal AppUserPrincipal → OrderService。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   Bearer JWT ──► principal.id ──► OrderService
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Role;
import com.example.orderservice.security.AppUserPrincipal;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 订单 REST API（需登录）。
 * <p>
 * {@link AuthenticationPrincipal} 从 JWT 过滤器解析出的 {@link AppUserPrincipal} 注入当前用户，
 * 避免客户端在 body 里伪造 userId。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** POST /api/orders — 创建订单，返回 201。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(principal.id(), request);
    }

    /** GET /api/orders — 当前用户的订单列表。 */
    @GetMapping
    public List<OrderResponse> getOrders(@AuthenticationPrincipal AppUserPrincipal principal) {
        return orderService.getOrders(principal.id());
    }

    /**
     * GET /api/orders/{id} — 单条订单。
     * 管理员可查任意订单；普通用户只能查自己的（否则 404）。
     */
    @GetMapping("/{id}")
    public OrderResponse getOrder(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id
    ) {
        if (principal.role() == Role.ADMIN) {
            return orderService.getOrderForAdmin(id);
        }
        return orderService.getOrderForUser(id, principal.id());
    }
}
