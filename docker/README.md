# docker

Compose 编排：PostgreSQL（多库初始化）、Kafka、五个 Spring Boot 服务、Angular 前端。

## 部署拓扑

```text
Browser → Angular (4200) --JWT/HTTP--> Order Service (8080) → order_db
                                          │
                                          ▼ order-created
                                        Kafka
                                          │
                                          ▼
                               Inventory Service (8083) → inventory_db
                                          │
                    inventory-reserved / inventory-reservation-failed
                          ┌───────────────┴───────────────┐
                          ▼                               ▼
                 Payment Service (8081)            Order Service（状态）
                          │ payment-completed / payment-failed
                          ├──────────────┬──────────────────┐
                          ▼              ▼                  ▼
                 Inventory（确认/释放）  Order            Notification (8082)
```

各服务职责与 Kafka 明细见各目录 `README.md` 与 [README.md](../README.md#架构与-saga)。

## 快速启动

在仓库根目录：

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

前端：http://localhost:4200  

若从旧版本升级且缺少 `inventory_db`，需重置卷一次：

```bash
docker compose -f docker/docker-compose.yml down --volumes
docker compose -f docker/docker-compose.yml up -d --build
```

## 端口

| 组件 | 地址 |
| --- | --- |
| Frontend | http://localhost:4200 |
| Order Service | http://localhost:8080 |
| Payment Service | http://localhost:8081 |
| Notification Service | http://localhost:8082 |
| Inventory Service | http://localhost:8083 |
| PostgreSQL | `localhost:5432`（user/password `postgres`） |
| Kafka | `localhost:9092` |

数据库初始化脚本：`postgres/init-databases.sql`（`order_db`、`inventory_db`、`payment_db` 等）。

运维命令、Kafka console、Outbox SQL：[docs/runbook.md](../docs/runbook.md)。
