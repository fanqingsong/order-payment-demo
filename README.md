# Order Payment Demo

Order Payment Demo is a small project for practicing a microservice-based order and payment workflow. Phase 1 provides only the repository structure and local PostgreSQL and Kafka infrastructure; application code has not been created yet.

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
