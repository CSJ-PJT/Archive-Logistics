# API Reference

Base URL:

```text
http://localhost:8092
```

## Health

```http
GET /actuator/health
GET /api/health
```

## Operations

```http
GET /api/operations/summary
```

Returns service status, active profile, event counts, route counts, outbox status, risk counts, ledger mode, and heap usage.

## Nexus Events

```http
POST /api/events/nexus
POST /api/events/nexus/bulk
```

Single event example:

```json
{
  "eventId": "evt-nexus-20260115-000123",
  "idempotencyKey": "NEXUS:LOGISTICS_DISPATCHED:FAC-A:SHIP-000123",
  "source": "Archive-Nexus",
  "eventType": "LOGISTICS_DISPATCHED",
  "occurredAt": "2026-01-15T10:32:15.000Z",
  "payload": {
    "factoryId": "FAC-A",
    "shipmentId": "SHIP-000123",
    "originCode": "FAC-A",
    "destinationCode": "DC-SEOUL-01",
    "priority": "HIGH",
    "itemType": "battery-module",
    "quantity": 120,
    "requiresColdChain": false
  }
}
```

## Simulation

```http
POST /api/simulations/shipments?count=100
```

Creates synthetic Nexus-like logistics events for local smoke and demo runs.

## Routes

```http
GET /api/routes/plans?page=0&size=20
GET /api/routes/plans/{routePlanId}
GET /api/routes/costs?page=0&size=20
GET /api/routes/costs/{routePlanId}
GET /api/routes/summary
GET /api/routes/summary?factoryId=FAC-A
GET /api/routes/summary?date=2026-01-15
GET /api/routes/summary?factoryId=FAC-A&date=2026-01-15
```

Pagination defaults to page size 50 and is capped by the service.

## Outbox

```http
GET /api/outbox/events
GET /api/outbox/events/{eventId}
GET /api/outbox/summary
POST /api/outbox/publish
POST /api/outbox/retry-failed
```

`POST /api/outbox/publish` returns dry-run output when Ledger is disabled.

## Batch

```http
POST /api/batch/outbox-publish/run
GET /api/batch/jobs
GET /api/batch/jobs/{executionId}
```

Used for explicit Spring Batch publisher execution and job inspection.

