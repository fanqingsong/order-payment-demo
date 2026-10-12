# Order Payment Demo

Event-driven demo: Angular + Spring Boot microservices + PostgreSQL + Kafka. Covers JWT auth, **transactional outbox**, inventory **Saga** (reserve → pay → confirm or release), idempotent consumers, and dead-letter topics.

**Stack:** Java 25, Spring Boot 4.1.1, Angular 22.1.x, PostgreSQL 17, Kafka 4.1.0.

## Quick start

From the repository root:

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

Open **http://localhost:4200**. Register, log in, create an order.

If you upgraded from an older version without `inventory_db`, reset volumes once: `docker compose -f docker/docker-compose.yml down --volumes`, then `up` again.

## Ports

| Component | URL |
| --- | --- |
| Frontend | http://localhost:4200 |
| Order Service | http://localhost:8080 |
| Payment Service | http://localhost:8081 |
| Notification Service | http://localhost:8082 |
| Inventory Service | http://localhost:8083 |
| PostgreSQL | `localhost:5432` (user/password `postgres`) |
| Kafka | `localhost:9092` |

Health: `http://localhost:<port>/actuator/health` on each Spring Boot service.

## Repository layout

```text
frontend/              Angular UI
order-service/         REST API, JWT, orders, outbox
inventory-service/     Stock reservation Saga step
payment-service/       Payment after inventory-reserved
notification-service/  Log-only “email” consumer
docker/                Compose + DB init
docs/                  Detailed documentation (start at docs/README.md)
```

## Documentation

| Doc | Content |
| --- | --- |
| **[docs/README.md](docs/README.md)** | Documentation index (中文) |
| **[docs/architecture.md](docs/architecture.md)** | Services, Saga sequence, order states, Kafka topics, demo rules |
| **[docs/code-guide.md](docs/code-guide.md)** | Reading order, class map, patterns ↔ code |
| **[docs/runbook.md](docs/runbook.md)** | Local/Docker run, Kafka/Postgres ops, tests |

Source comments in `src/main` (Java + `application.yml`) are in Chinese and align with **code-guide**.

## Tests

Docker must be running for Testcontainers integration tests.

```bash
cd order-service && ./mvnw test
cd inventory-service && ./mvnw test
cd payment-service && ./mvnw test
cd notification-service && ./mvnw test
cd frontend && npm test -- --watch=false
```

See [docs/runbook.md](docs/runbook.md) for step-by-step local run without full Compose.
