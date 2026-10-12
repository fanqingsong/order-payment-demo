# inventory-service

库存域微服务：Saga 的 **预占（Reserve）** 与支付后的 **确认（Confirm）/ 释放（Release）**。

| 项 | 值 |
| --- | --- |
| 端口 | **8083** |
| 数据库 | `inventory_db`（PostgreSQL） |
| 健康检查 | http://localhost:8083/actuator/health |

原则：**一服务一库**；与 order/payment 仅通过 Kafka JSON 事件协作。全局时序见 [README.md](../README.md#架构与-saga)。

## 在 Saga 中的位置

1. 消费 `order-created` → 预占库存（`RESERVED`）或缺货。
2. 成功 → 发布 `inventory-reserved`（**此后** payment-service 才会启动支付）。
3. 失败 → 发布 `inventory-reservation-failed`（订单终态，支付不会启动）。
4. 消费 `payment-completed` → **CONFIRM**（实扣）；消费 `payment-failed` → **RELEASE** 预占。

```text
Kafka: order-created → Inventory (8083) → inventory_db
              │
              ├── inventory-reserved / inventory-reservation-failed
              └── payment-completed / payment-failed → 确认或释放
```

## Kafka

| 方向 | Topic | 说明 |
| --- | --- | --- |
| 消费 | `order-created` | `OrderCreatedListener` → `InventoryService.reserveForOrder` |
| 生产 | `inventory-reserved` | 预占成功 |
| 生产 | `inventory-reservation-failed` | 预占失败 |
| 消费 | `payment-completed` | 确认扣减 |
| 消费 | `payment-failed` | 释放预占 |

消费幂等：`processed_events` + 业务逻辑。错误重试与 DLT 见 `KafkaConsumerErrorConfig`（`order-created.DLT`）。

## 本地事务

| 步骤 | 库 | 主要出站事件 |
| --- | --- | --- |
| 预占 | `inventory_db` | `inventory-reserved` / `inventory-reservation-failed` |
| 补偿 | `inventory_db` | 消费支付结果（无单独 release topic） |

## 演示规则（与本服务相关）

| 场景 | 用法 |
| --- | --- |
| 默认库存 | 首次出现的 `productName` 自动建 SKU，可售 **100** |
| 缺货 | 商品名 **`OUT_OF_STOCK`** → `inventory-reservation-failed` |
| 消费 DLT | 商品名 **`KAFKA_FAIL`** 于 `order-created` 消费失败 → `order-created.DLT`（重试 3 次，间隔 500ms） |

## 本地开发与测试

```bash
./mvnw spring-boot:run
./mvnw test
```

源码导读：[docs/code-guide.md](../docs/code-guide.md)（§ 库存）。
