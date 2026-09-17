package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.PaymentCompletedEvent;
import com.example.orderservice.event.PaymentFailedEvent;
import com.example.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentResultService {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultService.class);

    private final OrderRepository orderRepository;

    public PaymentResultService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void handleCompleted(PaymentCompletedEvent event) {
        updateOrder(event.eventId(), event.orderId(), event.paymentId(), OrderStatus.PAID);
    }

    @Transactional
    public void handleFailed(PaymentFailedEvent event) {
        updateOrder(event.eventId(), event.orderId(), event.paymentId(), OrderStatus.PAYMENT_FAILED);
    }

    private void updateOrder(UUID eventId, UUID orderId, UUID paymentId, OrderStatus targetStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.error("Cannot apply payment result because order was not found: eventId={}, orderId={}, paymentId={}",
                    eventId, orderId, paymentId);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Ignoring payment result for non-pending order: eventId={}, orderId={}, paymentId={}, status={}",
                    eventId, orderId, paymentId, order.getStatus());
            return;
        }

        order.updatePaymentStatus(targetStatus, Instant.now());
        log.info("Updated order from payment result: eventId={}, orderId={}, paymentId={}, status={}",
                eventId, orderId, paymentId, targetStatus);
    }
}
