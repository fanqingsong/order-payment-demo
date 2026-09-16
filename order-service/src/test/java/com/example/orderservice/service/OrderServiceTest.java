package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Test
    void createsOrderWithCalculatedTotalAndPendingPaymentStatus() {
        OrderService orderService = new OrderService(orderRepository);
        UUID userId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                "Mechanical Keyboard",
                3,
                new BigDecimal("49.95")
        );
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(userId, request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertThat(savedOrder.getUserId()).isEqualTo(userId);
        assertThat(savedOrder.getProductName()).isEqualTo("Mechanical Keyboard");
        assertThat(savedOrder.getQuantity()).isEqualTo(3);
        assertThat(savedOrder.getUnitPrice()).isEqualByComparingTo("49.95");
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("149.85");
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(savedOrder.getCreatedAt()).isNotNull();
        assertThat(savedOrder.getUpdatedAt()).isEqualTo(savedOrder.getCreatedAt());

        assertThat(response.totalAmount()).isEqualByComparingTo("149.85");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }
}
