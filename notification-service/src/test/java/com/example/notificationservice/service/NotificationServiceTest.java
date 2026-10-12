/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · NotificationServiceTest.java                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】NotificationService 日志输出测试。
 * 【为什么】回归 EMAIL SENT 文案。
 * 【怎么做】OutputCapture + AssertJ。
 *
 * 示意图（notification-service · 8082 · 无库）：
 *   service ──► 捕获日志
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class NotificationServiceTest {

    private final NotificationService notificationService = new NotificationService();

    @Test
    void logsSuccessfulPaymentEmail(CapturedOutput output) {
        UUID orderId = UUID.randomUUID();

        notificationService.sendPaymentSuccessful(orderId);

        assertThat(output).contains("EMAIL SENT", "Payment successful", "Order: " + orderId);
    }

    @Test
    void logsFailedPaymentEmail(CapturedOutput output) {
        UUID orderId = UUID.randomUUID();

        notificationService.sendPaymentFailed(orderId);

        assertThat(output).contains("EMAIL SENT", "Payment failed", "Order: " + orderId);
    }
}
