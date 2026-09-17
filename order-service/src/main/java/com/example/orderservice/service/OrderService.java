package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.repository.OutboxEventRepository;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        BigDecimal totalAmount = request.unitPrice().multiply(BigDecimal.valueOf(request.quantity()));
        Instant now = Instant.now();

        Order order = new Order(
                userId,
                request.productName(),
                request.quantity(),
                request.unitPrice(),
                totalAmount,
                OrderStatus.PENDING_PAYMENT,
                now,
                now
        );

        Order savedOrder = orderRepository.save(order);
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "OrderCreated",
                1,
                Instant.now(),
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getProductName(),
                savedOrder.getTotalAmount()
        );
        outboxEventRepository.save(new OutboxEvent(
                event.eventId(),
                savedOrder.getId(),
                event.eventType(),
                objectMapper.writeValueAsString(event),
                event.occurredAt()
        ));
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(UUID userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(UUID id, UUID userId) {
        return orderRepository.findByIdAndUserId(id, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForAdmin(UUID id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
