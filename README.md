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

Keep the consumer running and create an order through the frontend or `POST /api/orders`. You should see the order UUID key followed by a JSON `OrderCreated` event containing `eventId`, `eventVersion`, `occurredAt`, `orderId`, `userId`, and `amount`.
