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

- received events: 407
- processed events: 407
- failed events: 0
- route plans: 407
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

Ledger observed the publish:

- `eventsReceivedFromLogitics`: 124 -> 164
- `logisticsReceivedEvents`: 124 -> 164
- `transactions`: 181 -> 221
- Ledger status: HEALTHY

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

## ArchiveOS-Readable APIs

ArchiveOS and operators can observe Logistics through:

- `GET /`
- `GET /dashboard.html`
- `GET /api/operations/summary`
- `GET /api/routes/summary`
- `GET /api/outbox/summary`
- `GET /api/outbox/events`
- `POST /api/outbox/publish`
- `GET /actuator/health`

## Remaining Issues

- Docker image rebuild in this session exceeded the command timeout, but the app was successfully recreated with updated compose environment variables and verified against live Ledger.
- `integrationTest` test cases passed in the generated XML report, but the standalone Gradle task intermittently fails on Windows while deleting `build/test-results/integrationTest/binary/output.bin`. Required `test`, `build`, and `docker compose config` checks pass.
