/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · CreateOrderRequest.java                                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】POST /api/orders 请求体 record。
 * 【为什么】Bean Validation 校验商品名、数量、单价。
 * 【怎么做】@NotBlank/@Min/@DecimalMin 等注解。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   JSON ──@Valid──► createOrder
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建订单 HTTP 请求体。{@code @Valid} 触发 Bean Validation，失败由 {@link com.example.orderservice.exception.GlobalExceptionHandler} 返回 400。
 */
public record CreateOrderRequest(
        @NotBlank @Size(max = 255) String productName,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal unitPrice
) {
}
