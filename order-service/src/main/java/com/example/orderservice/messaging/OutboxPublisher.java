/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OutboxPublisher.java                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】定时扫描 PENDING Outbox 并投递 Kafka。
 * 【为什么】实现 Transactional Outbox，支持 Kafka 短暂不可用重试。
 * 【怎么做】@Scheduled + 反序列化 payload + publishOrderCreated().get() 成功后 markPublished。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   ┌─────────┐  poll   ┌───────┐
 *   │ outbox  │───────►│ Kafka │
 *   └─────────┘        └───────┘
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.messaging;

import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.entity.OutboxStatus;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * 事务性发件箱的「投递员」：定时拉取 {@link OutboxStatus#PENDING} 记录，发送到 Kafka。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>与 {@link com.example.orderservice.service.OrderService#createOrder} 解耦：下单事务只写库，发消息在此异步重试</li>
 *   <li>Kafka 暂时不可用时记录保持 PENDING，下次调度继续尝试</li>
 *   <li>仅当 {@code kafkaTemplate.send(...).get()} 成功后才 {@link OutboxEvent#markPublished}</li>
 * </ul>
 * 测试可通过 {@code outbox.publisher.enabled=false} 关闭定时任务。
 */
@Component
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OrderEventPublisher orderEventPublisher,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * 默认每 1 秒执行一次（{@code outbox.publisher.fixed-delay-ms}）。
     * 每次最多处理 100 条，按创建时间 FIFO，避免单条失败阻塞整批时可配合后续优化。
     */
    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
                .forEach(this::publish);
    }

    private void publish(OutboxEvent outboxEvent) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(
                    outboxEvent.getPayload(),
                    OrderCreatedEvent.class
            );
            // 阻塞等待发送结果：演示环境简化处理；生产环境可考虑异步 + 更细的状态机
            orderEventPublisher.publishOrderCreated(event).get();
            outboxEvent.markPublished(Instant.now());
            log.info("Published outbox event: eventId={}, orderId={}",
                    outboxEvent.getId(), outboxEvent.getAggregateId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Outbox publishing interrupted; event remains pending: eventId={}, orderId={}",
                    outboxEvent.getId(), outboxEvent.getAggregateId(), exception);
        } catch (Exception exception) {
            // 失败不 markPublished，下次调度重试
            log.warn("Failed to publish outbox event; event remains pending: eventId={}, orderId={}",
                    outboxEvent.getId(), outboxEvent.getAggregateId(), exception);
        }
    }
}
