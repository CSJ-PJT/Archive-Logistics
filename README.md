# Archive-Logitics

Archive-Logitics is a Java 21 / Spring Boot backend for synthetic logistics events in the Archive Platform Ecosystem. The repository name intentionally keeps `Logitics`; Java packages, domain terms, and event names use correct `logistics`.

It receives logistics-related events from Archive-Nexus, calculates deterministic synthetic route plans, ETA, risk, delay/deviation flags, and logistics cost, then stores an outbox event that Archive-Ledger can ingest as a cost-confirmed transaction.

## Ecosystem Role

- Archive-Nexus creates manufacturing and shipment cause events.
- Archive-Logitics converts those events into synthetic logistics route/cost facts.
- Archive-Ledger receives Ledger-compatible events at `/api/events/nexus/bulk` and creates transactions, ledger entries, settlement, and reconciliation data.
- ArchiveOS handles approval, audit, RAG evidence, and notification flows for high-risk cases.

Route-Atlas remains separate because this service is not a map/routing engine. Archive-Logitics owns deterministic synthetic logistics event processing and accounting handoff; it does not call real map APIs or use real delivery, vehicle, user location, address, phone, card, or account data.

## Stack

Java 21, Spring Boot 3, Spring Web, Validation, JPA, PostgreSQL, Flyway, Spring Batch, Actuator, Micrometer/Prometheus, springdoc-openapi, JUnit 5, AssertJ, Testcontainers, Docker Compose, GitHub Actions.

Lombok, Kafka, Redis, real routing engines, and real logistics data are not used.

## Local Run

Start PostgreSQL first:

```powershell
docker compose up archive-logitics-postgres -d
```

Run the app:

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
.\gradlew.bat bootRun
```

Testcontainers-backed checks are split out for local/CI stability:

```powershell
.\gradlew.bat integrationTest
```

Default local endpoints:

- App: `http://localhost:8092`
- PostgreSQL: `localhost:15434`
- OpenAPI UI: `http://localhost:8092/swagger-ui.html`

## Docker Compose

```powershell
docker compose up --build
```

Compose includes only Archive-Logitics and its PostgreSQL database. Archive-Ledger is an external service. In Docker, use:

```powershell
$env:ARCHIVE_LEDGER_BASE_URL="http://host.docker.internal:18080"
```

## Key APIs

- `POST /api/events/nexus`
- `POST /api/events/nexus/bulk`
- `POST /api/simulations/shipments?count=100`
- `GET /api/routes/plans`
- `GET /api/routes/plans/{routePlanId}`
- `GET /api/routes/costs`
- `GET /api/routes/costs/{routePlanId}`
- `GET /api/routes/summary?date=YYYY-MM-DD`
- `GET /api/outbox/events`
- `GET /api/outbox/events/{eventId}`
- `GET /api/outbox/summary`
- `POST /api/outbox/publish`
- `POST /api/outbox/retry-failed`
- `POST /api/batch/outbox-publish/run`
- `GET /api/batch/jobs`
- `GET /api/operations/summary`
- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## Ledger Compatibility Mode

Default mode is `ARCHIVE_LEDGER_V1_COMPAT`.

```yaml
archive:
  ledger:
    enabled: false
    base-url: http://localhost:18080
    bulk-endpoint: /api/events/nexus/bulk
    contract-mode: ARCHIVE_LEDGER_V1_COMPAT
```

In this mode, Archive-Logitics publishes a JSON array to Archive-Ledger's existing `/api/events/nexus/bulk` API. The output `eventType` is `LOGISTICS_DISPATCHED`, `source` is `Archive-Logitics`, and payload includes `estimatedCost`, `totalCost`, `vendorId`, `severity`, and `requiresApproval` for Ledger normalization.

`LOGISTICS_CONFIRMED_NATIVE` is reserved for a future Ledger endpoint such as `/api/events/logistics/bulk`.

## Outbox and Batch

Incoming events are processed into `route_plan`, `route_cost`, and `logistics_outbox_event` in one event-level transaction. Ledger publishing is isolated through a PostgreSQL DB Outbox. Ledger downtime does not stop ingestion.

When `archive.ledger.enabled=false`, `/api/outbox/publish` returns `DRY_RUN` and does not modify outbox status. When enabled, the publisher posts to Ledger and marks events `PUBLISHED`, `RETRY`, or `FAILED`.

Spring Batch exposes `outboxPublishJob` through:

```powershell
curl.exe -X POST http://localhost:8092/api/batch/outbox-publish/run
```

The scheduler is off by default and only runs when `archive.outbox.scheduler.enabled=true`.

## Quick Demo

```powershell
curl.exe http://localhost:8092/actuator/health
curl.exe http://localhost:8092/api/operations/summary
curl.exe -X POST "http://localhost:8092/api/simulations/shipments?count=100"
curl.exe http://localhost:8092/api/outbox/summary
curl.exe -X POST http://localhost:8092/api/outbox/publish
curl.exe "http://localhost:8092/api/routes/plans?page=0&size=20"
```

## OCI-lite Profile

`application-oci-lite.yml` reduces pool and batch pressure for later OCI free-tier deployment. Example:

```powershell
$env:SPRING_PROFILES_ACTIVE="oci-lite"
$env:JAVA_OPTS="-Xms128m -Xmx384m"
$env:DB_POOL_SIZE="3"
$env:ARCHIVE_OUTBOX_CHUNK_SIZE="20"
$env:ARCHIVE_OUTBOX_SCHEDULER_ENABLED="false"
```

## Portfolio Line

Archive-Logitics - Java/Spring Synthetic Logistics Event Backend: implemented a PostgreSQL/Flyway Outbox-based logistics event service that converts Archive-Nexus shipment events into deterministic synthetic route/cost facts and publishes Ledger-compatible cost events with idempotency, batch publishing, failure isolation, audit logs, and Actuator operations APIs.
