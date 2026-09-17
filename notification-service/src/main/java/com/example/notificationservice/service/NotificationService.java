package com.example.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendPaymentSuccessful(UUID orderId) {
        log.info("EMAIL SENT\nPayment successful\nOrder: {}", orderId);
    }

    public void sendPaymentFailed(UUID orderId) {
        log.info("EMAIL SENT\nPayment failed\nOrder: {}", orderId);
    }
}
