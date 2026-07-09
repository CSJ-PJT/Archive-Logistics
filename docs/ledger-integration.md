# Ledger Integration

Archive-Logistics publishes logistics cost-confirmed events to Archive-Ledger through the PostgreSQL outbox publisher.

## Default Local Contract

```env
ARCHIVE_LEDGER_ENABLED=true
ARCHIVE_LEDGER_BASE_URL=http://localhost:8093
ARCHIVE_LEDGER_BULK_ENDPOINT=/api/events/logistics/bulk
ARCHIVE_LEDGER_CONTRACT_MODE=LOGISTICS_CONFIRMED_NATIVE
ARCHIVE_LEDGER_PUBLISH_TIMEOUT_MS=30000
ARCHIVE_OUTBOX_SCHEDULER_ENABLED=false
```

Docker uses this host URL by default:

```env
ARCHIVE_LEDGER_BASE_URL=http://host.docker.internal:8093
```

## Publish Flow

1. Archive-Nexus sends a logistics event to Archive-Logistics.
2. Archive-Logistics calculates route, ETA, cost, delay, deviation, and approval risk.
3. A Ledger-compatible event is saved to `logistics_outbox_event`.
4. `POST /api/outbox/publish` or `outboxPublishJob` reads publishable rows.
5. The publisher sends a native bulk request to Archive-Ledger.
6. Successful or duplicate Ledger responses mark outbox rows as `PUBLISHED`.
7. Connectivity or contract failures move rows to `RETRY` or `FAILED` with `last_error`.

## Native Request Shape

```json
{
  "source": "Archive-Logitics",
  "events": [
    {
      "eventId": "evt-logitics-20260115-000456",
      "idempotencyKey": "LOGISTICS:LOGISTICS_COST_CONFIRMED:ROUTE-000456",
      "source": "Archive-Logitics",
      "eventType": "LOGISTICS_COST_CONFIRMED",
      "aggregateType": "ROUTE_PLAN",
      "aggregateId": "ROUTE-000456",
      "schemaVersion": 1,
      "occurredAt": "2026-01-15T10:45:00Z",
      "payload": {
        "routePlanId": "ROUTE-000456",
        "shipmentId": "SHIP-000123",
        "factoryId": "FAC-A",
        "vendorId": "VENDOR-LOGISTICS-01",
        "totalCost": 93420,
        "currency": "KRW",
        "riskScore": 0.42,
        "requiresApproval": false
      }
    }
  ]
}
```

`Archive-Logitics` is kept as the internal event source literal for Ledger compatibility. External service naming remains `Archive-Logistics`.

## Settlement and Reconciliation Readiness

Archive-Logistics does not create Ledger settlement or reconciliation tables. It prepares the input events that Ledger uses to create:

- `finance_transaction`
- `ledger_entry`
- `settlement_batch`
- `settlement_detail`
- `reconciliation_result`

After a successful publish, verify Ledger:

```powershell
curl.exe "http://localhost:8093/api/events/received?source=Archive-Logitics"
curl.exe "http://localhost:8093/api/transactions?source=Archive-Logitics"
curl.exe "http://localhost:8093/api/ledger/summary?source=Archive-Logitics"
curl.exe -X POST "http://localhost:8093/api/settlements/daily/run?date=2026-01-15"
curl.exe -X POST "http://localhost:8093/api/reconciliation/daily?date=2026-01-15"
curl.exe http://localhost:8093/api/reconciliation/summary
```

## Failure Modes

- Ledger disabled: publish returns `DRY_RUN` and does not mutate outbox rows.
- Ledger unreachable: publish returns a failure summary; rows move to `RETRY` or `FAILED`.
- Duplicate Ledger event: duplicate is treated as successful for idempotent publish completion.
- Scheduler disabled: outbox remains pending until manual publish or explicit batch run.
