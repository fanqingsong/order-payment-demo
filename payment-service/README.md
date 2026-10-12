# payment-service

支付域微服务：在 **库存预占成功之后** 模拟支付，发布支付结果事件。

| 项 | 值 |
| --- | --- |
| 端口 | **8081** |
| 数据库 | `payment_db`（PostgreSQL） |
| 健康检查 | http://localhost:8081/actuator/health |

全局 Saga 见 [README.md](../README.md#架构与-saga)。本服务 **不** 订阅 `order-created`，**仅** 消费 `inventory-reserved`，避免「付了款没货」。

## 在 Saga 中的位置

```text
inventory-reserved → Payment (8081) → payment_db
                          │
                          ├── payment-completed
                          └── payment-failed
                                ↓
                    inventory（确认/释放）、order（状态）、notification（日志）
```

与 order、inventory 并行消费不同 topic；支付步骤只在本服务 DB 内提交本地事务，失败由库存 **RELEASE** 补偿。

## Kafka

| 方向 | Topic | 说明 |
| --- | --- | --- |
| 消费 | `inventory-reserved` | `InventoryReservedListener` → `PaymentService.processInventoryReserved` |
| 生产 | `payment-completed` | 支付成功 |
| 生产 | `payment-failed` | 支付失败 |

**不消费** `order-created`。

消费幂等：`ProcessedEvent` + `PaymentService`。DLT：`inventory-reserved.DLT`（`KafkaConsumerErrorConfig`）。

## 本地事务

| 步骤 | 库 | 主要出站事件 |
| --- | --- | --- |
| 支付 | `payment_db` | `payment-completed` / `payment-failed` |

## 演示规则（与本服务相关）

| 场景 | 用法 |
| --- | --- |
| 支付限额 | 订单总金额 **≤ 10000** 成功，否则 `payment-failed` |
| 支付消费 DLT | 对 **`inventory-reserved`** 消息可用 `KAFKA_FAIL` 钩子 → `inventory-reserved.DLT`；UI 下单在库存阶段用 `KAFKA_FAIL` 会先失败，需测试或手工发消息验证 |

## 本地开发与测试

```bash
./mvnw spring-boot:run
./mvnw test
```

源码导读：[docs/code-guide.md](../docs/code-guide.md)（§ 支付）。
