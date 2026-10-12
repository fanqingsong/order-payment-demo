# notification-service

通知微服务：消费支付结果，**日志模拟邮件**（无数据库）。

| 项 | 值 |
| --- | --- |
| 端口 | **8082** |
| 数据库 | 无 |
| 健康检查 | http://localhost:8082/actuator/health |

与 order、inventory 一样订阅支付结果 topic，各自独立 **consumer group**。架构总览见 [README.md](../README.md#架构与-saga)。

## 在 Saga 中的位置

支付完成后并行通知，不参与库存或订单状态写入：

```text
payment-completed / payment-failed
        │
        ├── inventory-service（确认/释放）
        ├── order-service（PAID / PAYMENT_FAILED）
        └── notification-service（日志「发邮件」）
```

## Kafka

| 方向 | Topic | 说明 |
| --- | --- | --- |
| 消费 | `payment-completed` | `PaymentResultListener` → `NotificationService` |
| 消费 | `payment-failed` | 同上 |

## 本地开发与测试

```bash
./mvnw spring-boot:run
./mvnw test
```

源码导读：[docs/code-guide.md](../docs/code-guide.md)（§ 订单回写与通知）。
