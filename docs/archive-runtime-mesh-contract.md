# Archive Runtime Mesh V1 Contract

## Scope

Archive-Logistics exposes persisted Synthetic Runtime Data to ArchiveOS Console V3. It does not expose actual delivery locations, people, vehicles, payment instruments, or credentials.

## Read APIs

```http
GET /api/runtime/status
GET /api/runtime-events/recent?limit=100
GET /api/runtime-events/recent?after={cursor}&limit=100
GET /api/runtime-events/correlation/{correlationId}
GET /api/runtime-events/entity/{entityId}
GET /api/operations/summary
GET /api/workforce/summary
GET /api/productivity/summary
GET /api/capacity/summary
```

All endpoints are read-only. They do not insert seed data, run simulations, publish an outbox, or run settlement work.

## Runtime Event Shape

Every projection supplies these fields:

```json
{
  "eventId": "...",
  "idempotencyKey": "...",
  "sourceService": "Archive-Logistics",
  "targetService": "ArchiveOS",
  "domain": "logistics",
  "eventType": "ROUTE_ASSIGNED",
  "entityType": "route_plan",
  "entityId": "ROUTE-...",
  "correlationId": "...",
  "causationId": "...",
  "simulationRunId": "...",
  "settlementCycleId": "...",
  "workdayId": "...",
  "status": "COMPLETED",
  "severity": "INFO",
  "occurredAt": "2026-07-11T10:00:00",
  "hopCount": 0,
  "maxHop": 5,
  "cursor": "opaque-runtime-position",
  "metadata": {}
}
```

Absent optional lineage values are returned as `null`. Projection-only events use a deterministic `RUNTIME:{eventType}:{entityId}` idempotency key. `metadata` contains only synthetic IDs, synthetic categories, and operational counters.

Archive-Logistics additionally persists its shipment lifecycle projection in `shipment_runtime_event`. The current lifecycle types are `SHIPMENT_CREATED`, `ROUTE_ASSIGNED`, `ROUTE_COST_CALCULATED`, `TRUCK_DISPATCHED`, `DELIVERY_IN_TRANSIT`, `DELIVERY_DELAYED`, `DELIVERY_COMPLETED`, `COLD_CHAIN_RISK_DETECTED`, and `LOGISTICS_COST_CONFIRMED`.

## Cursor Pull

`recent` without `after` returns the newest events first for dashboard bootstrap. `recent?after={cursor}` returns events strictly after the opaque cursor in ascending `(occurredAt, eventId)` order. Each event includes its own opaque `cursor`: ArchiveOS retains the first cursor from a bootstrap response, then retains the last cursor from an ascending incremental response.

An invalid cursor returns an empty list with HTTP 200; it never causes service work or data mutation.

## No Data Semantics

When no persisted workforce workday result exists:

- workforce, productivity, and capacity summaries return HTTP 200;
- `available=false` and `status=NO_DATA` are returned by all workforce, productivity, and capacity summaries;
- numerical counters are zero only as an explicit no-data representation;
- `reason` / `degradedReason` explains that no persisted synthetic result exists.

This lets ArchiveOS distinguish a service with no runtime data from a service that processed zero events.
