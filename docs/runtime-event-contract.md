# Runtime Event Contract

ArchiveOS Live Flow collects Archive-Logistics runtime projections through read-only APIs. These APIs expose persisted Synthetic Runtime Data only and never create actual delivery, personal, address, or financial data.

## APIs

```http
GET /api/runtime-events/recent?limit=100
GET /api/runtime-events/recent?after={cursor}&limit=100
GET /api/runtime-events/correlation/{correlationId}
GET /api/runtime-events/entity/{entityId}
```

## Common Response Fields

```json
{
  "eventId": "evt-logistics-20260711-001",
  "idempotencyKey": "LOGISTICS:LOGISTICS_COST_CONFIRMED:ROUTE-001",
  "sourceService": "Archive-Logistics",
  "targetService": "Archive-Ledger",
  "domain": "logistics",
  "eventType": "LOGISTICS_COST_CONFIRMED",
  "entityType": "route_plan",
  "entityId": "ROUTE-001",
  "correlationId": "CORR-001",
  "causationId": "CAUSE-001",
  "simulationRunId": "SIM-001",
  "settlementCycleId": "CYCLE-001",
  "workdayId": null,
  "status": "PROCESSING",
  "severity": "INFO",
  "displayLabel": "Logistics cost event prepared for Ledger",
  "occurredAt": "2026-07-11T10:00:00",
  "hopCount": 0,
  "maxHop": 5,
  "cursor": "opaque-runtime-position",
  "metadata": {
    "routePlanId": "ROUTE-001",
    "shipmentId": "SHIP-001",
    "orderId": "ORD-001"
  }
}
```

`status` and `severity` follow Runtime Mesh V1 uppercase values. UI labels can be localized, but event types, IDs, enum values, API paths, and cursor values are not translated.

## Metadata Rules

Metadata contains only synthetic IDs, synthetic code categories, and operational counters. It must not contain names, phone numbers, addresses, card/account numbers, payment tokens, secrets, passwords, webhooks, or private keys.

## Cursor Semantics

The initial `recent` response is newest-first. A request using `after` receives only newer events in ascending `(occurredAt, eventId)` order. Each event carries its opaque cursor; retain the first bootstrap cursor or the last incremental cursor. Invalid cursors safely return an empty result.
