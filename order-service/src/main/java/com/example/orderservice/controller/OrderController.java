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

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(principal.id(), request);
    }

    @GetMapping
    public List<OrderResponse> getOrders(@AuthenticationPrincipal AppUserPrincipal principal) {
        return orderService.getOrders(principal.id());
    }

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
