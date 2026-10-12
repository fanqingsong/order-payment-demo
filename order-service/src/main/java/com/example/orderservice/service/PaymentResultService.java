/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentResultService.java                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】根据支付 Kafka 事件更新 Order。
 * 【为什么】完成 Saga 支付腿在 order_db 的状态。
 * 【怎么做】handleCompleted/Failed；允许 PENDING_PAYMENT 或乱序 PENDING_INVENTORY。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   payment event ──► PAID | PAYMENT_FAILED
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
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

/**
 * 将 Kafka 中的支付结果「落地」到订单表。
 * <p>
 * 当订单为 {@link OrderStatus#PENDING_PAYMENT}，或库存已预占但支付结果先到达（{@link OrderStatus#PENDING_INVENTORY}）时更新。
 */
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
        // 幂等/防护：已支付或已失败的订单不再接受支付结果（除非业务定义更复杂的状态机）
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                && order.getStatus() != OrderStatus.PENDING_INVENTORY) {
            log.warn("Ignoring payment result for non-pending order: eventId={}, orderId={}, paymentId={}, status={}",
                    eventId, orderId, paymentId, order.getStatus());
            return;
        }

        order.updatePaymentStatus(targetStatus, Instant.now());
        log.info("Updated order from payment result: eventId={}, orderId={}, paymentId={}, status={}",
                eventId, orderId, paymentId, targetStatus);
    }
}
