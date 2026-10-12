# order-service

订单域微服务：**REST + JWT**、下单、**事务性 Outbox** 发布 `order-created`，并消费库存/支付结果更新订单状态。

| 项 | 值 |
| --- | --- |
| 端口 | **8080** |
| 数据库 | `order_db`（PostgreSQL） |
| 健康检查 | http://localhost:8080/actuator/health |

全局 Saga 序列图、订单状态机与 Kafka 总表见 [README.md](../README.md#架构与-saga)。

## 在 Saga 中的位置

业务顺序为 **Reserve → Pay → Confirm / Release**（先占库存，再支付）。本服务负责：

1. **下单**：本地事务写入 `orders`（`PENDING_INVENTORY`）+ `outbox_events`，事务内**不**调 Kafka。
2. **Outbox 异步发件**：`OutboxPublisher` 定时任务将 outbox 发往 `order-created`。
3. **消费库存结果**：`inventory-reserved` → `PENDING_PAYMENT`；`inventory-reservation-failed` → `INVENTORY_FAILED`。
4. **消费支付结果**：`payment-completed` → `PAID`；`payment-failed` → `PAYMENT_FAILED`（含与库存消息的乱序防护）。

```text
Browser --JWT/HTTP--> Order Service (8080) → order_db
                          │ order-created (Outbox)
                          ▼
                        Kafka → inventory-service …
                          ▲
                          └── inventory-* / payment-* 回写订单
```

## Kafka

| 方向 | Topic | 说明 |
| --- | --- | --- |
| 生产 | `order-created` | Outbox 发布；key 一般为 `orderId` |
| 消费 | `inventory-reserved` | 预占成功 |
| 消费 | `inventory-reservation-failed` | 预占失败 |
| 消费 | `payment-completed` | 支付成功 |
| 消费 | `payment-failed` | 支付失败 |

跨服务契约：本服务 `event/*Event.java` 字段需与其它服务一致。

## 订单状态

| 状态 | 含义 |
| --- | --- |
| `PENDING_INVENTORY` | 已下单，等待预占 |
| `PENDING_PAYMENT` | 已预占，等待支付 |
| `INVENTORY_FAILED` | 缺货等，终态（不会进入支付） |
| `PAID` | 支付成功 |
| `PAYMENT_FAILED` | 支付失败 |

实现要点：`Order.applyInventoryReserved`、`applyInventoryFailed`、`updatePaymentStatus`；乱序时 `PaymentResultService` 可在 `PENDING_INVENTORY` 上直接应用支付结果。

## 事务性 Outbox（下单）

下单与写入 `outbox_events` 在同一 PostgreSQL 事务；重复发送时 `eventId` 不变，下游幂等。Outbox SQL 与运维说明见 [docs/runbook.md](../docs/runbook.md#outbox-表)。

## 本地开发与测试

```bash
./mvnw spring-boot:run
./mvnw test   # 需 Docker（Testcontainers）
```

源码导读：[docs/code-guide.md](../docs/code-guide.md)（HTTP/安全、Outbox、库存/支付回写）。
