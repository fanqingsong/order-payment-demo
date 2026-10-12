/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentResultListenerTest.java                             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】PaymentResultListener  wiring 测试。
 * 【为什么】确保监听器调用 NotificationService。
 * 【怎么做】Mock NotificationService + verify。
 *
 * 示意图（notification-service · 8082 · 无库）：
 *   listener ──verify──► send*
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.notificationservice.messaging;

import com.example.notificationservice.event.PaymentCompletedEvent;
import com.example.notificationservice.event.PaymentFailedEvent;
import com.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentResultListenerTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final PaymentResultListener listener = new PaymentResultListener(notificationService);

    @Test
    void sendsSuccessfulPaymentNotification() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(), "PaymentCompleted", 1, Instant.now(),
                UUID.randomUUID(), orderId, new BigDecimal("99.90")
        );

        listener.onPaymentCompleted(event);

        verify(notificationService).sendPaymentSuccessful(orderId);
    }

    @Test
    void sendsFailedPaymentNotification() {
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(), "PaymentFailed", 1, Instant.now(),
                UUID.randomUUID(), orderId, new BigDecimal("10000.01")
        );

        listener.onPaymentFailed(event);

        verify(notificationService).sendPaymentFailed(orderId);
    }
}
