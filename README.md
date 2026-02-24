# specmatic-ibm-mq-sample

Cross-platform sample project that implements the async workflow described in `spec/spec.yaml` using IBM MQ.

## What it does

Queues from the spec are implemented as IBM MQ local queues:

- `new-orders` -> consumes `OrderRequest`, publishes `Order` to `wip-orders`
- `to-be-cancelled-orders` -> consumes `CancelOrderRequest`, publishes `CancellationReference` to `cancelled-orders`
- `wip-orders` -> consumes `Order`, publishes `OrderAccepted` to `accepted-orders`
- `out-for-delivery-orders` -> consumes `OutForDelivery` and updates in-memory order status

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

The app connects to IBM MQ at `localhost:1415` (matching `spec/spec.yaml`).

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
