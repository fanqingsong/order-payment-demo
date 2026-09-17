package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.PaymentCompletedEvent;
import com.example.orderservice.event.PaymentFailedEvent;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentResultServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Test
    void changesPendingOrderToPaidForCompletedPayment() {
        UUID orderId = UUID.randomUUID();
        Order order = pendingOrder();
        Instant previousUpdatedAt = order.getUpdatedAt();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        PaymentResultService service = new PaymentResultService(orderRepository);

        service.handleCompleted(completedEvent(orderId));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getUpdatedAt()).isAfterOrEqualTo(previousUpdatedAt);
    }

    @Test
    void changesPendingOrderToPaymentFailedForFailedPayment() {
        UUID orderId = UUID.randomUUID();
        Order order = pendingOrder();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        PaymentResultService service = new PaymentResultService(orderRepository);

        service.handleFailed(failedEvent(orderId));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    @Test
    void handlesMissingOrderWithoutThrowing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        PaymentResultService service = new PaymentResultService(orderRepository);

        assertThatCode(() -> service.handleCompleted(completedEvent(orderId))).doesNotThrowAnyException();
    }

    @Test
    void ignoresDuplicateResultForOrderThatIsNoLongerPending() {
        UUID orderId = UUID.randomUUID();
        Order order = pendingOrder();
        order.updatePaymentStatus(OrderStatus.PAID, Instant.now());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        PaymentResultService service = new PaymentResultService(orderRepository);

        service.handleFailed(failedEvent(orderId));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    private Order pendingOrder() {
        Instant now = Instant.now().minusSeconds(1);
        return new Order(
                UUID.randomUUID(),
                "Mechanical Keyboard",
                1,
                new BigDecimal("99.90"),
                new BigDecimal("99.90"),
                OrderStatus.PENDING_PAYMENT,
                now,
                now
        );
    }

    private PaymentCompletedEvent completedEvent(UUID orderId) {
        return new PaymentCompletedEvent(
                UUID.randomUUID(),
                "PaymentCompleted",
                1,
                Instant.now(),
                UUID.randomUUID(),
                orderId,
                new BigDecimal("99.90")
        );
    }

    private PaymentFailedEvent failedEvent(UUID orderId) {
        return new PaymentFailedEvent(
                UUID.randomUUID(),
                "PaymentFailed",
                1,
                Instant.now(),
                UUID.randomUUID(),
                orderId,
                new BigDecimal("10000.01")
        );
    }
}
