# Final Smoke Result

Date: 2026-07-09

## Role

Archive-Logistics receives Archive-Nexus shipment/logistics events, calculates deterministic synthetic route, ETA, cost, delay, deviation, and risk, then publishes Logistics cost-confirmed events to Archive-Ledger through the PostgreSQL outbox.

## Route Summary

`/api/routes/summary` is healthy after the nullable JPQL parameter fix.

Verified endpoints:

- `GET /api/routes/summary`: HTTP 200
- `GET /api/routes/summary?factoryId=FAC-A`: HTTP 200
- `GET /api/routes/summary?date=2026-01-15`: HTTP 200
- `GET /api/routes/summary?factoryId=FAC-A&date=2026-01-15`: HTTP 200

Current smoke state:

- received events: 609
- processed events: 609
- failed events: 0
- route plans: 609
- service status: HEALTHY

## Ledger Publish

Runtime configuration:

- `ledger.enabled=true`
- `baseUrl=http://host.docker.internal:18080`
- `bulkEndpoint=/api/events/logistics/bulk`
- `contractMode=LOGISTICS_CONFIRMED_NATIVE`
- `publishTimeoutMs=30000`
- scheduler disabled by default

Real publish result after increasing the Ledger publish timeout:

```json
{
  "requestedCount": 50,
  "publishedCount": 50,
  "failedCount": 0,
  "retriedCount": 0,
  "skippedCount": 0,
  "status": "SUCCESS"
}
```

Latest real publish result after the Nexus settlement batch feature was added:

```json
{
  "requestedCount": 50,
  "publishedCount": 50,
  "failedCount": 0,
  "retriedCount": 0,
  "skippedCount": 0,
  "status": "SUCCESS"
}
```

Current outbox snapshot while external event generation was still active:

- `pending`: 99
- `published`: 510
- `failed`: 0
- `retry`: 0
- `skipped`: 0

Ledger observed Logistics publish:

- `eventsReceivedFromLogitics`: 124 -> 164
- `logisticsReceivedEvents`: 124 -> 164
- `transactions`: 181 -> 221
- Ledger status: HEALTHY

Latest Ledger operations snapshot:

- `eventsReceivedFromLogitics`: 524
- `logisticsReceivedEvents`: 524
- `transactions`: 834
- `failed`: 0

An earlier 3 second timeout run proved failure isolation: Ledger accepted part of the request, while Archive-Logistics marked the chunk as `RETRY` and remained healthy. The default timeout was increased to 30 seconds to avoid false retry on normal local Ledger processing.

## Settlement and Reconciliation

Archive-Ledger downstream verification for `2026-07-09`:

- `POST /api/settlements/daily/run?date=2026-07-09`: SUCCESS
- settlement batch: `SET-20260709-13154677`
- settlement transaction count: 36
- settlement total amount: 17,426,676 KRW
- `POST /api/reconciliation/daily?date=2026-07-09`: OK
- latest reconciliation mismatch: 0

Reconciliation summary:

```json
{
  "date": "2026-07-09",
  "nexusEvents": 57,
  "receivedEvents": 221,
  "createdTransactions": 221,
  "logisticsEventCount": 164,
  "directEventCount": 57,
  "logisticsTransactionCount": 64,
  "directTransactionCount": 57,
  "duplicates": 345,
  "failed": 0,
  "approvalRequired": 113,
  "settlementReady": 3,
  "settled": 105,
  "mismatch": 0,
  "status": "OK"
}
```

## Nexus Daily Settlement Callback

Archive-Logistics now supports a daily manufacturing compensation callback to Archive-Nexus after Logistics cost events are published to Ledger.

Smoke command:

```powershell
curl.exe -X POST "http://localhost:8092/api/settlements/nexus-daily/run?date=2026-07-09"
```

Result:

- requested factories: 3
- sent: 3
- dry-run: 0
- retry: 0
- failed: 0
- duplicate rerun: duplicate-safe, no second callback
- total settled shipments: 460
- total logistics cost: 127,669,490 KRW
- manufacturing impact cost: 38,300,847 KRW

Nexus observed callback:

```json
{
  "total": 3,
  "received": 3,
  "invalid": 0,
  "totalLogisticsCost": 127669490.00,
  "manufacturingImpactCost": 38300847.00
}
```

Operational note: run the daily settlement after the target date publish cut-off. Already `SENT` factory/date settlements are immutable to avoid duplicate manufacturing compensation.

## ArchiveOS-Readable APIs

ArchiveOS and operators can observe Logistics through:

- `GET /`
- `GET /dashboard.html`
- `GET /api/operations/summary`
- `GET /api/routes/summary`
- `GET /api/outbox/summary`
- `GET /api/outbox/events`
- `POST /api/outbox/publish`
- `POST /api/settlements/nexus-daily/run`
- `GET /api/settlements/nexus-daily/summary`
- `GET /actuator/health`

## Remaining Issues

- External event generation was still active during smoke, so outbox pending can increase again after a successful publish. The daily Nexus settlement should run after the operational cut-off.
- `integrationTest` test cases passed in the generated XML report, but the standalone Gradle task intermittently fails on Windows while deleting `build/test-results/integrationTest/binary/output.bin`. Required `test`, `build`, and `docker compose config` checks pass.
