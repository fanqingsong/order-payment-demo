/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · NotificationService.java                                   ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】用日志模拟发送邮件的服务。
 * 【为什么】compose 日志中可观察 Saga 终态副作用。
 * 【怎么做】SLF4J info 输出 EMAIL SENT 模板。
 *
 * 示意图（notification-service · 8082 · 无库）：
 *   事件 ──► log "EMAIL SENT"
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 通知领域服务。真实项目会调用邮件/SMS API；此处用日志模拟，便于在 docker compose 日志里看到完整链路。
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** 支付成功通知（模拟邮件正文）。 */
    public void sendPaymentSuccessful(UUID orderId) {
        log.info("EMAIL SENT\nPayment successful\nOrder: {}", orderId);
    }

    /** 支付失败通知。 */
    public void sendPaymentFailed(UUID orderId) {
        log.info("EMAIL SENT\nPayment failed\nOrder: {}", orderId);
    }
}
