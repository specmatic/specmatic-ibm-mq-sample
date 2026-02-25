# specmatic-ibm-mq-sample

Cross-platform sample project that implements the async workflow described in `spec/spec.yaml` using IBM MQ.

## What it does

Queues from the spec are implemented as IBM MQ local queues:

- `new.orders` -> consumes `OrderRequest`, publishes `Order` to `wip.orders`
- `to.be.cancelled.orders` -> consumes `CancelOrderRequest`, publishes `CancellationReference` to `cancelled.orders`
- `out.for.delivery.orders` -> consumes `OutForDelivery` and updates in-memory order status
- `accepted.orders` -> published when the HTTP `PUT /orders` API is called

Message payloads are JSON, and the optional header `orderCorrelationId` is preserved on produced messages.

## Tech stack (why this is cross-platform)

- Java 21 + Spring Boot 3 (runs on macOS, Linux, Windows)
- IBM MQ Java client (`com.ibm.mq.allclient`) which avoids OS-specific native client setup
- Docker Compose for local IBM MQ broker (same startup command across OSes)

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop / Docker Engine with Compose

## Run locally

1. Start IBM MQ:

```bash
docker compose up -d
```

Apple Silicon (`arm64`) note:

- IBM MQ's official container is currently `amd64` only.
- This project sets `platform: linux/amd64` in `docker-compose.yml`, so Docker Desktop runs it using emulation.
- If Docker still fails on macOS ARM, enable Docker Desktop x86/amd64 emulation (Rosetta/QEMU support), then retry.

2. Start the app:

```bash
mvn spring-boot:run
```

The app starts an HTTP server on port `8080` and connects to IBM MQ at `localhost:1415` (matching `spec/spec.yaml`).

## Easy smoke test (recommended)

Run this in a second terminal while the app is running:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.smoke-test.enabled=true --spring.jms.listener.auto-startup=false"
```

Shortcut scripts:

- macOS/Linux: `sh scripts/smoke-test.sh`
- Windows PowerShell: `powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1`

What it does:

- Publishes a sample `new-orders` message
- Waits for `accepted-orders`
- Publishes a sample cancel request
- Waits for `cancelled-orders`
- Publishes an `out-for-delivery-orders` message
- Exits with status `0` on success / `1` on failure

## Unit tests

```bash
mvn test
```

These test the core message-processing logic without needing IBM MQ.

## Queue and connection defaults

- Queue manager: `QM1`
- Channel: `DEV.APP.SVRCONN`
- Username: `app`
- Password: `passw0rd`
- Host port mapping: `1415 -> 1414` (container MQ listener)

## Notes

- IBM MQ container startup can take a little time on first run.
- On ARM machines (for example Apple Silicon Macs), IBM MQ runs under `amd64` emulation in Docker.
- The sample uses simple logging and an in-memory status store; replace with a database if you need persistence.

## Architecture

- Spring Boot app with JMS listeners for `new.orders`, `to.be.cancelled.orders`, and `out.for.delivery.orders`
- IBM MQ broker in Docker (queue manager `QM1`)
- HTTP API for accepting orders (`PUT /orders`) and checking status (`GET /orders/{id}`)
- In-memory `OrderStateStore` to track the latest order status and update time

## Contract tests

Run the Specmatic contract tests in Docker:

```bash
./scripts/run-contract-tests.sh
```

This uses `docker-compose.test.yaml`, starts IBM MQ, the app, and Specmatic, streams live logs only from `specmatic-test`, then returns the Specmatic exit code.
The stack is left running for debugging; use the cleanup script to tear down.

To show logs for all containers after the run, set `SHOW_ALL_LOGS=1`:

```bash
SHOW_ALL_LOGS=1 ./scripts/run-contract-tests.sh
```

Direct compose command (same behavior):

```bash
docker compose -f docker-compose.test.yaml up --abort-on-container-exit --exit-code-from specmatic-test
```

Cleanup (stop/remove containers and volumes):

```bash
./scripts/cleanup-contract-tests.sh
```

## What happens in the contract tests

- Specmatic publishes a `new.orders` message and expects a `wip.orders` message.
- Specmatic calls `PUT /orders` (before fixture) and expects an `accepted.orders` message with the same payload.
- Specmatic publishes a `to.be.cancelled.orders` message and expects a `cancelled.orders` message.
- Specmatic publishes an `out.for.delivery.orders` message and verifies order status via `GET /orders/{id}?status=SHIPPED` (after fixture).

## Debugging test failures

- Check the Specmatic container logs: `docker compose -f docker-compose.test.yaml logs -f specmatic-test`
- Check the app logs for listener/validation errors: `docker compose -f docker-compose.test.yaml logs -f app`
- Check IBM MQ logs if messages are not flowing: `docker compose -f docker-compose.test.yaml logs -f ibm-mq`
- If HTTP fixtures fail, confirm the app is reachable at `http://sut:8080` from the Specmatic container.
- Use the message count report from Specmatic to confirm which queue is missing messages.

## Run the app manually

1. Start IBM MQ:

```bash
docker compose up -d
```

2. Run the app:

```bash
mvn spring-boot:run
```

3. Test the API:

```bash
curl -i -X PUT http://localhost:8080/orders \\
  -H 'Content-Type: application/json' \\
  -d '{\"id\":123,\"status\":\"ACCEPTED\",\"timestamp\":\"2025-04-12T14:30:00Z\"}'

curl -i http://localhost:8080/orders/123?status=ACCEPTED
```
