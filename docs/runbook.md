# 运行与运维

架构说明见 [architecture.md](architecture.md)；源码入口见 [code-guide.md](code-guide.md)。

## 前置依赖

- Java 25、Docker（Compose）、Node/npm（前端）
- 集成测试需要 Docker 运行 Testcontainers

## 方式 A：Compose 一键启动

仓库根目录：

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

UI：http://localhost:4200

停止并保留数据：

```bash
docker compose -f docker/docker-compose.yml down
```

**删除所有本地库与 Kafka 数据**（含重建 `inventory_db`）：

```bash
docker compose -f docker/docker-compose.yml down --volumes
```

## 方式 B：仅基础设施 + 本地进程

只起 Postgres 与 Kafka：

```bash
docker compose -f docker/docker-compose.yml up -d postgres kafka
```

在各目录执行 `./mvnw spring-boot:run`（建议顺序）：

1. `order-service/` → 8080  
2. `inventory-service/` → 8083  
3. `payment-service/` → 8081  
4. `notification-service/` → 8082  

前端：

```bash
cd frontend && npm install && npm start
```

开发代理将 `/api` 转到 `localhost:8080`。

## 端口与数据库

| 组件 | 地址 |
| --- | --- |
| 前端 | http://localhost:4200 |
| order / inventory / payment / notification | 8080 / 8083 / 8081 / 8082 |
| PostgreSQL | `localhost:5432`，用户/密码 `postgres` |
| Kafka | `localhost:9092` |

| 数据库 | 服务 |
| --- | --- |
| `order_db` | order-service |
| `inventory_db` | inventory-service |
| `payment_db` | payment-service |

连接示例：

```bash
psql -h localhost -p 5432 -U postgres -d order_db
```

## 基本使用流程

1. 注册、登录（JWT）。
2. 创建订单 → `PENDING_INVENTORY` + Outbox。
3. 库存预占 → `inventory-reserved` 或失败。
4. 支付服务**仅**在 `inventory-reserved` 后扣款演示。
5. 支付结果驱动库存确认/释放与订单终态。

详细时序见 [architecture.md](architecture.md#推荐业务顺序先占库存再支付)。

## Kafka：查看消息

容器名默认 `order-payment-kafka`。消费示例（打印 key）：

**order-created**

```bash
docker exec -it order-payment-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-created \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | "
```

**inventory-reserved**

```bash
docker exec -it order-payment-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic inventory-reserved \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | "
```

**DLT**

```bash
# 库存消费 order-created 失败（商品名 KAFKA_FAIL）
docker exec -it order-payment-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-created.DLT \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | "

# 支付消费 inventory-reserved 失败（通常需测试或手工消息）
docker exec -it order-payment-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic inventory-reserved.DLT \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | "
```

描述主题：

```bash
docker exec order-payment-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic order-created
```

## Outbox 表

```bash
docker exec -it order-payment-postgres psql \
  -U postgres \
  -d order_db \
  -c "SELECT id, aggregate_id, event_type, status, created_at, published_at FROM outbox_events ORDER BY created_at DESC;"
```

## 测试

```bash
cd order-service && ./mvnw test
cd inventory-service && ./mvnw test
cd payment-service && ./mvnw test
cd notification-service && ./mvnw test
cd frontend && npm test -- --watch=false
cd frontend && npm run build
```

## 停止本地进程

各终端 `Ctrl+C` 停止 Spring Boot 与 `npm start`，再：

```bash
docker compose -f docker/docker-compose.yml down
```
