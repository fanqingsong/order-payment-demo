/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderService.java                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订单 CRUD 核心；createOrder 同事务写 Outbox。
 * 【为什么】避免双写：不在下单事务内直调 Kafka。
 * 【怎么做】计算金额 → save Order(PENDING_INVENTORY) → save Outbox(JSON OrderCreated)。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   POST order
 *      │
 *      ▼
 *   TX[ orders + outbox ]
 *      └──► (异步) order-created
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
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

/**
 * 订单领域核心服务：持久化订单，并在<strong>同一数据库事务</strong>内写入 Outbox 事件。
 * <p>
 * 创建订单时<strong>不会直接调用 Kafka</strong>，避免「库已提交但消息没发出去」或相反的不一致；
 * 真正发 Kafka 由 {@link com.example.orderservice.messaging.OutboxPublisher} 异步完成。
 */
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

    /**
     * 创建订单：计算金额 → 保存订单（待支付）→ 写入 Outbox（待发布 OrderCreated）。
     * 三步在同一 {@link Transactional} 事务中，要么全部成功，要么全部回滚。
     */
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
                OrderStatus.PENDING_INVENTORY,
                now,
                now
        );

        Order savedOrder = orderRepository.save(order);

        // 构造领域事件：eventId 用于下游幂等；orderId 作为 Kafka 消息 key，保证同订单有序
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "OrderCreated",
                1,
                Instant.now(),
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getProductName(),
                savedOrder.getQuantity(),
                savedOrder.getTotalAmount()
        );

        // Outbox 表与 orders 表同事务提交；payload 存 JSON，便于 OutboxPublisher 反序列化后发送
        outboxEventRepository.save(new OutboxEvent(
                event.eventId(),
                savedOrder.getId(),
                event.eventType(),
                objectMapper.writeValueAsString(event),
                event.occurredAt()
        ));
        return toResponse(savedOrder);
    }

    /** 当前登录用户的订单列表，按创建时间倒序。 */
    // 否：readOnly=true 不是 Repeatable Read。它只标记只读事务（JDBC setReadOnly、Hibernate 常设 FlushMode=MANUAL 并跳过脏检查）。
    // 未指定 isolation 时沿用数据源默认隔离级别（PostgreSQL 一般为 READ_COMMITTED）。
    // 若要 RR：@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)。共享锁另需 @Lock(PESSIMISTIC_READ)。
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(UUID userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 普通用户只能查自己的订单；查不到抛 {@link OrderNotFoundException}（对外 404）。 */
    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(UUID id, UUID userId) {
        return orderRepository.findByIdAndUserId(id, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /** 管理员可按订单 ID 查询任意用户的订单。 */
    @Transactional(readOnly = true)
    public OrderResponse getOrderForAdmin(UUID id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /** 管理后台：全部订单。 */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 实体 → API 响应 DTO，避免把 JPA 实体直接暴露给 HTTP 层。 */
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
