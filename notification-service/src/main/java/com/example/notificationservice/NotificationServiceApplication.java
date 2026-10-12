/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · NotificationServiceApplication.java                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】通知微服务 Spring Boot 启动类。
 * 【为什么】演示 Saga 旁路：只消费支付结果，模拟邮件。
 * 【怎么做】@SpringBootApplication + main。
 *
 * 示意图（notification-service · 8082 · 无库）：
 *   Boot ──► Kafka Consumer only
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通知服务（Notification Service）启动入口。
 * <p>
 * 订阅支付结果 Kafka 主题，在日志中模拟发送邮件通知。
 * 真实系统中这里会对接 SMTP、短信网关或推送服务；本 demo 只打印 {@code EMAIL SENT} 便于观察链路。
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
