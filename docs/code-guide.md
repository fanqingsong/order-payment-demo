# 源码导读

配合各服务 **`src/main` 内中文注释** 与 `application.yml` 阅读。各服务边界与 Kafka 见对应目录 **README.md**；跨服务序列图见 [README.md](../README.md#架构与-saga)。不要与本文重复展开类级导读。

## 端到端：类与 Topic 对照

```text
POST /api/orders
  OrderController → OrderService.createOrder          [order_db, PENDING_INVENTORY + outbox]
  OutboxPublisher → OrderEventPublisher               → order-created

  OrderCreatedListener → InventoryService.reserveForOrder
                        → inventory-reserved | inventory-reservation-failed

  InventoryResultListener → InventoryResultService    [order: PENDING_PAYMENT | INVENTORY_FAILED]
  InventoryReservedListener → PaymentService.processInventoryReserved
                        → payment-completed | payment-failed

  PaymentResultListener (inventory) → confirm / release
  PaymentResultListener (order)     → PAID | PAYMENT_FAILED
  PaymentResultListener (notification) → 日志
```

## 建议阅读顺序

### 1. 启动类

- `order-service/.../OrderServiceApplication.java` — 定时任务驱动 Outbox
- `inventory-service/.../InventoryServiceApplication.java`
- `payment-service/.../PaymentServiceApplication.java`
- `notification-service/.../NotificationServiceApplication.java`

### 2. HTTP 与安全（order-service）

1. `controller/AuthController.java`、`OrderController.java`、`AdminOrderController.java`
2. `config/SecurityConfig.java`
3. `security/JwtAuthenticationFilter.java`、`JwtService.java`、`AppUserPrincipal.java`、`AppUserDetailsService.java`
4. `service/AuthService.java`

### 3. 下单与 Outbox

1. `dto/CreateOrderRequest.java`、`OrderResponse.java`
2. `entity/Order.java`、`OrderStatus.java`
3. `service/OrderService.java`
4. `entity/OutboxEvent.java`、`OutboxStatus.java`
5. `event/OrderCreatedEvent.java`
6. `messaging/OutboxPublisher.java`、`OrderEventPublisher.java`、`KafkaTopics.java`、`KafkaTopicConfig.java`

Outbox 说明见 [README.md#事务性-outbox下单](../README.md#事务性-outbox下单) 与 [runbook.md#outbox-表](runbook.md#outbox-表)。

### 4. 库存（inventory-service）

1. `messaging/OrderCreatedListener.java`
2. `service/InventoryService.java`
3. `entity/InventoryItem.java`、`InventoryReservation.java`、`ProcessedEvent.java`
4. `messaging/InventoryEventPublisher.java`、`PaymentResultListener.java`
5. `config/KafkaConsumerErrorConfig.java`、`KafkaTopicConfig.java`

### 5. 支付（payment-service）

1. `messaging/InventoryReservedListener.java`（不消费 `order-created`）
2. `service/PaymentService.java` — `processInventoryReserved`
3. `entity/Payment.java`、`ProcessedEvent.java`、`PaymentStatus.java`
4. `messaging/PaymentEventPublisher.java`
5. `config/KafkaConsumerErrorConfig.java`、`KafkaTopicConfig.java`

### 6. 订单回写与通知

**order-service：** `InventoryResultListener`、`InventoryResultService`；`PaymentResultListener`、`PaymentResultService`；对应 `event/*`。

**notification-service：** `PaymentResultListener`、`NotificationService`。

### 7. 横切

- `exception/GlobalExceptionHandler.java`（order-service）
- 各服务 `repository/*`

`src/test` 无教学注释；主流程理清后再看测试。

## 概念 ↔ 代码

| 概念 | 主要位置 |
| --- | --- |
| 事务性 Outbox | `OrderService`、`OutboxPublisher` |
| Saga 预占/确认/释放 | `InventoryService` |
| 支付在预占之后 | `InventoryReservedListener`、`PaymentService` |
| 消费幂等 | `ProcessedEvent` + inventory/payment `Service` |
| Kafka 契约 | 各服务 `event/` 包 |
| DLT | 两服务的 `KafkaConsumerErrorConfig` |
| 订单状态防护 | `Order`、`InventoryResultService`、`PaymentResultService` |

## 目录速查

```text
order-service/.../orderservice/
  controller/  security/  service/  entity/  event/  messaging/  dto/  repository/

inventory-service/.../inventoryservice/
  service/  messaging/  config/  entity/  event/  repository/

payment-service/.../paymentservice/
  service/  messaging/  config/  entity/  event/  repository/

notification-service/.../notificationservice/
  messaging/  service/  event/
```

## 调试提示

- Outbox 默认定时约 1s；测试可关 `outbox.publisher.enabled=false`（见 `OutboxPublisher` 注释）。
- 启动顺序建议：**order → inventory → payment → notification → frontend**（见 [runbook.md](runbook.md)）。
- 升级后缺 `inventory_db` 时需 `docker compose ... down --volumes` 重建卷。
