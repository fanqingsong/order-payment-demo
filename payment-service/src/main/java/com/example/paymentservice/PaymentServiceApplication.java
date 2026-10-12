/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentServiceApplication.java                             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】支付微服务 Spring Boot 启动类。
 * 【为什么】无 HTTP，仅 Kafka 消费 inventory-reserved 并发布支付结果。
 * 【怎么做】标准 main 启动。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   Boot ──► Kafka in/out only
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 支付服务（Payment Service）启动入口。
 * <p>
 * 本服务<strong>不提供 HTTP 业务 API</strong>，完全通过 Kafka 与订单服务协作：
 * <ol>
 *   <li>订阅 {@code inventory-reserved}（库存预占成功后），模拟扣款并写入 {@code payment_db}</li>
 *   <li>向 {@code payment-completed} / {@code payment-failed} 发布支付结果</li>
 * </ol>
 * 演示中还包含：幂等消费（{@code processed_events}）、消费失败重试与死信队列（DLT）。
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
