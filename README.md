# Order Payment Demo

## Project overview

Order Payment Demo is a small learning project that demonstrates how an Angular frontend, Spring Boot microservices, Spring Security, PostgreSQL, and Apache Kafka work together in an event-driven order and payment workflow.

The project deliberately stays small enough to study. It includes practical examples of service-owned databases, JWT authentication, role-based authorization, asynchronous Kafka communication, retry and dead-letter handling, idempotent consumers, Flyway migrations, Testcontainers integration tests, and the Transactional Outbox Pattern.

The main versions currently configured in the repository are Java 25, Spring Boot 4.1.1, Angular 22.1.x, PostgreSQL 17, and Apache Kafka 4.1.0.

## Architecture

- **Angular frontend** provides registration, login, and order screens. It sends JWT-authenticated HTTP requests to the Order Service.
- **Order Service** owns users and orders in `order_db`, exposes the REST API, writes `OrderCreated` events to its transactional outbox, publishes them to Kafka, and consumes payment results to update order status.
- **Payment Service** owns payments and processed-event records in `payment_db`. It consumes order events, applies the demo payment rule, and publishes payment results.
- **Notification Service** has no database. It consumes payment results and simulates sending email by writing messages to its log.
- **PostgreSQL** stores each stateful service's data in a separate service-owned database.
- **Kafka** carries events between services without allowing one service to query another service's database.

```text
Browser
  |
  v
Angular (4200) --HTTP/JWT--> Order Service (8080) --> order_db
                              |
                              | order-created
                              v
                            Kafka (9092)
                              |
                              v
                       Payment Service (8081) --> payment_db
                              |
                              | payment-completed / payment-failed
                              +----------------------+
                              |                      |
                              v                      v
                    Order Service             Notification Service (8082)
                    updates status            logs simulated email
```

## Main features

- User registration and login with BCrypt password hashing and JWT access tokens
- `USER` and `ADMIN` role-based authorization
- User-owned order listing and details, with an admin endpoint for all orders
- Order creation with server-calculated totals
- Simple payment processing: amounts up to `10000` complete; larger amounts fail
- Kafka communication through `order-created`, `payment-completed`, and `payment-failed`
- Notification consumers that log simulated success or failure emails
- Three payment-consumer retries followed by the `order-created.DLT` dead-letter topic
- Idempotent Payment Service processing using the `processed_events` table
- Transactional Outbox delivery of `OrderCreated` events
- Flyway-managed schemas and Hibernate schema validation
- Focused unit, security, and Testcontainers integration tests

## Prerequisites

Install these tools before running the project:

- **Java 25** — configured by all three Maven projects
- **Angular CLI 22.1.x** — installed locally by `npm install`; a global Angular CLI installation is not required because the npm scripts use the local CLI
- **Docker with Docker Compose** — runs PostgreSQL 17 and Kafka 4.1.0 and is also required by the Testcontainers integration tests
- **Git** — for cloning and version control;

## Project structure

```text
order-payment-demo/
├── frontend/              # Angular 22 standalone application
├── order-service/         # REST API, authentication, orders, and outbox
├── payment-service/       # Kafka payment processing and payment database
├── notification-service/  # Kafka notification consumers; no database
├── docker/                # Docker Compose and PostgreSQL initialization
├── docs/                  # Project documentation placeholder
└── README.md
```

## How to run the project

Run each application in a separate terminal. Start the infrastructure first so that the services can connect to PostgreSQL and Kafka.

### Step 1: Start PostgreSQL and Kafka

Working directory: repository root

```bash
docker compose -f docker/docker-compose.yml up -d
```

PostgreSQL listens on port `5432`, and Kafka listens on port `9092`.

### Step 2: Start the Order Service

Working directory: `order-service/`

```bash
cd order-service
./mvnw spring-boot:run
```

The Order Service starts on port `8080`.

### Step 3: Start the Payment Service

Working directory: `payment-service/`

```bash
cd payment-service
./mvnw spring-boot:run
```

The Payment Service starts on port `8081`.

### Step 4: Start the Notification Service

Working directory: `notification-service/`

```bash
cd notification-service
./mvnw spring-boot:run
```

The Notification Service starts on port `8082`.

### Step 5: Start the Angular frontend

Working directory: `frontend/`

Install the frontend dependencies the first time:

```bash
cd frontend
npm install
```

Start the Angular development server using the configured `start` script:

```bash
npm start
```

The frontend starts on port `4200`. Its development proxy forwards `/api` requests to the Order Service at `localhost:8080`.

## Service URLs

| Component | URL or connection |
| --- | --- |
| Angular frontend | `http://localhost:4200` |
| Order Service | `http://localhost:8080` |
| Payment Service | `http://localhost:8081` |
| Notification Service | `http://localhost:8082` |
| PostgreSQL | `localhost:5432` |
| Kafka bootstrap server | `localhost:9092` |

The Spring Boot Actuator health endpoint for each service is available at `/actuator/health` on that service's port.

## Basic usage flow

1. Open `http://localhost:4200/register` and create a user account.
2. Log in. The frontend stores the returned JWT and includes it in subsequent Order Service requests.
3. Create an order. The Order Service saves the order with `PENDING_PAYMENT` status and saves an `OrderCreated` event in `outbox_events` in the same database transaction.
4. The outbox publisher sends the event to the Kafka `order-created` topic.
5. The Payment Service consumes the event and creates one payment. An amount up to `10000` becomes `COMPLETED`; an amount above `10000` becomes `FAILED`.
6. The Payment Service publishes either `PaymentCompleted` to `payment-completed` or `PaymentFailed` to `payment-failed`.
7. The Order Service consumes the result and changes the order status to `PAID` or `PAYMENT_FAILED`.
8. The Notification Service consumes the same result and logs a simulated payment-successful or payment-failed email.

## Local infrastructure

Run these commands from the repository root.

Start PostgreSQL and Kafka in the background:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Check the running containers:

```bash
docker compose -f docker/docker-compose.yml ps
```

Stop and remove the containers while keeping their data volumes:

```bash
docker compose -f docker/docker-compose.yml down
```

To also delete all locally persisted PostgreSQL and Kafka data, add `--volumes` to the stop command.

## PostgreSQL connection

- Host: `localhost`
- Port: `5432`
- Username: `postgres`
- Password: `postgres`
- Order Service database: `order_db`
- Payment Service database: `payment_db`

The databases are created automatically the first time the PostgreSQL volume is initialized. PostgreSQL data is retained in the `postgres_data` Docker volume.

Example connection:

```bash
psql -h localhost -p 5432 -U postgres -d order_db
```

## Kafka connection

- Bootstrap server: `localhost:9092`
- Mode: single-node KRaft (no ZooKeeper)

Kafka data is retained in the `kafka_data` Docker volume.

### Inspecting order-created events

The Order Service creates and publishes JSON events to the `order-created` topic. Start Kafka and the Order Service, then describe the topic with:

```bash
docker exec order-payment-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic order-created
```

Consume existing and new messages while printing each order ID message key:

```bash
docker exec -it order-payment-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-created \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | "
```

Keep the consumer running and create an order through the frontend or `POST /api/orders`. You should see the order UUID key followed by a JSON `OrderCreated` event containing `eventId`, `eventVersion`, `occurredAt`, `orderId`, `userId`, `productName`, and `amount`.

### Inspecting failed order events

Create an order whose product name is exactly `KAFKA_FAIL` to simulate a Payment Service consumer failure. The Payment Service retries processing three times with a 500 ms delay, then publishes the original event to `order-created.DLT`.

Inspect existing and new dead-letter messages with:

```bash
docker exec -it order-payment-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-created.DLT \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | "
```

The Payment Service logs the initial failure, each retry, and the final transfer to the dead-letter topic. The message key remains the order UUID.

## Transactional outbox

Order creation uses a transactional outbox to prevent an order from being committed without a corresponding `OrderCreated` event waiting to be published. The Order Service saves the order and a serialized event in `outbox_events` in the same PostgreSQL transaction. It does not contact Kafka during that transaction.

A scheduled publisher checks pending outbox records every second. After Kafka confirms a send, the publisher marks the record as `PUBLISHED` and sets `published_at`. If Kafka is unavailable, the record remains `PENDING` and is retried on a later run.

This provides at-least-once delivery. A failure after Kafka accepts a message but before the database records it as published can cause the same event to be sent again. The event retains its original `eventId`, allowing the Payment Service's idempotent consumer to ignore that duplicate safely.

Inspect the outbox with:

```bash
docker exec -it order-payment-postgres psql \
  -U postgres \
  -d order_db \
  -c "SELECT id, aggregate_id, event_type, status, created_at, published_at FROM outbox_events ORDER BY created_at DESC;"
```

## Testing

Docker must be running for integration tests that use Testcontainers.

Run the complete Order Service test suite from `order-service/`:

```bash
cd order-service
./mvnw test
```

Run the complete Payment Service test suite from `payment-service/`:

```bash
cd payment-service
./mvnw test
```

Run the complete Notification Service test suite from `notification-service/`:

```bash
cd notification-service
./mvnw test
```

Run the Angular tests once from `frontend/`:

```bash
cd frontend
npm test -- --watch=false
```

Build the production frontend bundle with:

```bash
npm run build
```

## Stopping the project

Stop each foreground Spring Boot service and the Angular development server by pressing `Ctrl+C` in the terminal where it is running.

After the applications have stopped, run this from the repository root to stop PostgreSQL and Kafka while preserving their data:

```bash
docker compose -f docker/docker-compose.yml down
```

Only when you intentionally want to delete all local PostgreSQL and Kafka data, including the Order and Payment databases, remove the Docker volumes as well:

```bash
docker compose -f docker/docker-compose.yml down --volumes
```

The volume-removal command is destructive. The next infrastructure startup creates empty databases again and Flyway rebuilds the service schemas when the services start.
