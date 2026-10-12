/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderServiceApplication.java                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订单微服务 Spring Boot 启动类。
 * 【为什么】聚合 REST、JWT、Outbox 定时任务、Kafka 消费/生产。
 * 【怎么做】@SpringBootApplication + @EnableScheduling（驱动 OutboxPublisher）。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   ┌──────────┐
 *   │ :8080    │── HTTP + Outbox + Kafka
 *   └──────────┘
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单服务（Order Service）启动入口。
 * <p>
 * 在本演示项目中，订单服务是<strong>对外 REST API 的网关</strong>，负责：
 * <ul>
 *   <li>用户注册 / 登录（JWT）</li>
 *   <li>创建与查询订单</li>
 *   <li>通过「事务性发件箱（Transactional Outbox）」可靠地发布 {@code OrderCreated} 事件到 Kafka</li>
 *   <li>消费支付结果事件，更新订单状态（{@code PAID} / {@code PAYMENT_FAILED}）</li>
 * </ul>
 * {@link EnableScheduling} 用于驱动 {@code OutboxPublisher} 定时扫描待发事件。
 */
@SpringBootApplication
@EnableScheduling
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
