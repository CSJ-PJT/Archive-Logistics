# Nexus Daily Settlement

Archive-Logistics can create a daily manufacturing compensation callback for Archive-Nexus after logistics cost events have been published to Archive-Ledger.

This keeps the operational order explicit:

```text
Nexus shipment event -> Logistics route/cost -> Logistics outbox -> Ledger publish -> Nexus daily settlement callback
```

## Settlement Basis

The settlement batch only includes route plans whose Logistics outbox event is already `PUBLISHED`.

Included rows:

- `route_plan.created_at` is within the settlement date
- `route_plan.factory_id` matches the selected factory
- matching `logistics_outbox_event.aggregate_id = route_plan.route_plan_id`
- matching outbox status is `PUBLISHED`

This prevents Archive-Logistics from compensating Nexus for logistics costs that have not yet been delivered to Archive-Ledger.

Operationally, run this batch after the daily logistics cut-off and after publishable outbox rows for the target date have been drained. In automatic demo mode, the scheduler checks this condition and runs only when every route outbox row for the target date is `PUBLISHED`. A `SENT` settlement is treated as immutable to avoid duplicate manufacturing compensation callbacks. Late-arriving events should be handled by the next operational adjustment flow or by running the daily batch only after the ingestion window closes.

## Calculation

For each settlement date and factory:

- `totalShipments`: count of published route plans
- `delayedShipments`: published routes where `delayed=true`
- `heldShipments`: original Nexus event type is `SHIPMENT_HOLD_RELEASED`
- `totalQuantity`: sum of original Nexus event payload `quantity`
- `totalLogisticsCost`: sum of `route_cost.total_cost`
- `manufacturingImpactCost`: `round(totalLogisticsCost * manufacturingShareRate)`
- `onTimeRate`: `(totalShipments - delayedShipments) / totalShipments`

Default manufacturing share:

```yaml
archive:
  nexus-settlement:
    manufacturing-share-rate: 0.3000
```

## APIs

```http
POST /api/settlements/nexus-daily/run
POST /api/settlements/nexus-daily/run?date=2026-07-09
POST /api/settlements/nexus-daily/run?date=2026-07-09&factoryId=FAC-A

GET /api/settlements/nexus-daily
GET /api/settlements/nexus-daily?date=2026-07-09
GET /api/settlements/nexus-daily/summary
GET /api/settlements/nexus-daily/summary?date=2026-07-09
GET /api/settlements/nexus-daily/{settlementId}
```

Spring Batch:

```http
POST /api/batch/nexus-daily-settlement/run
POST /api/batch/nexus-daily-settlement/run?date=2026-07-09
POST /api/batch/nexus-daily-settlement/run?date=2026-07-09&factoryId=FAC-A
```

## Nexus Callback

When enabled, Archive-Logistics sends:

```http
POST http://localhost:8080/api/logistics/settlements/daily
```

Payload key fields:

- `settlementId`: `LGS-SETTLE-{yyyyMMdd}-{factoryId}`
- `idempotencyKey`: `LOGISTICS:DAILY:{settlementDate}:{factoryId}`
- `source`: `Archive-Logistics`
- `settlementDate`
- `factoryId`
- `totalLogisticsCost`
- `manufacturingImpactCost`
- `onTimeRate`
- `evidence`
- `payload`

## Configuration

```yaml
archive:
  nexus-settlement:
    enabled: true
    base-url: http://localhost:8080
    daily-endpoint: /api/logistics/settlements/daily
    publish-timeout-ms: 30000
    max-retry-count: 5
    scheduler:
      enabled: true
      fixed-delay-ms: 300000
      initial-delay-ms: 45000
      date-offset-days: 0
    manufacturing-share-rate: 0.3000
```

Docker uses `http://host.docker.internal:8080` by default so the Logistics container can call a Nexus service running on the host.

## Failure Handling

- `enabled=false`: settlement rows are stored as `DRY_RUN`.
- Nexus unavailable: row becomes `RETRY` with `retry_count`, `last_error`, and `next_retry_at`.
- retry count reaches max: row becomes `FAILED`.
- already sent settlement: duplicate-safe response with no second external payment callback.

## Smoke

```powershell
curl.exe -X POST http://localhost:8092/api/outbox/publish
curl.exe -X POST "http://localhost:8092/api/settlements/nexus-daily/run?date=2026-07-09"
curl.exe http://localhost:8092/api/settlements/nexus-daily/summary
curl.exe "http://localhost:8080/api/logistics/settlements/summary"
```
