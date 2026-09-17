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
