package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.event.OrderCreatedEvent;
import com.example.paymentservice.event.PaymentCompletedEvent;
import com.example.paymentservice.event.PaymentFailedEvent;
import com.example.paymentservice.messaging.PaymentEventPublisher;
import com.example.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal PAYMENT_LIMIT = new BigDecimal("10000");

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processOrder(OrderCreatedEvent orderEvent) {
        PaymentStatus status = orderEvent.amount().compareTo(PAYMENT_LIMIT) <= 0
                ? PaymentStatus.COMPLETED
                : PaymentStatus.FAILED;
        Instant now = Instant.now();
        Payment payment = new Payment(orderEvent.orderId(), orderEvent.amount(), status, now, now);
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Processed payment: eventId={}, orderId={}, paymentId={}, status={}",
                orderEvent.eventId(), orderEvent.orderId(), savedPayment.getId(), savedPayment.getStatus());

        if (status == PaymentStatus.COMPLETED) {
            eventPublisher.publishCompleted(new PaymentCompletedEvent(
                    UUID.randomUUID(),
                    "PaymentCompleted",
                    1,
                    Instant.now(),
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getAmount()
            ));
        } else {
            eventPublisher.publishFailed(new PaymentFailedEvent(
                    UUID.randomUUID(),
                    "PaymentFailed",
                    1,
                    Instant.now(),
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getAmount()
            ));
        }
    }
}
