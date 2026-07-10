# ArchiveOS Live Flow Contract

## Purpose

ArchiveOS Live Flow reads Archive-Logistics runtime state through read-only APIs.
Archive-Logistics continues to run even when ArchiveOS is unavailable.

The projection is built from persisted synthetic runtime data only:

- Nexus logistics events
- route plans
- route costs
- Logistics outbox events
- workforce allocations
- workday productivity results

It does not create fake animation-only trucks, tokens, addresses, or personal data.

## Read-only APIs

```http
GET /api/runtime-events/recent?limit=100
GET /api/runtime-events/correlation/{correlationId}
GET /api/runtime-events/entity/{entityId}
GET /api/workforce/summary
GET /api/productivity/summary
GET /api/capacity/summary
GET /api/operations/summary
GET /api/logistics-economy/summary
GET /api/outbox/summary
```

## Operations Summary Additions

`GET /api/operations/summary` includes Live Flow fields:

- `serviceName`
- `serviceRole`
- `latestEventAt`
- `liveFlowAvailable`
- `shipmentsRequested`
- `shipmentsDispatched`
- `shipmentsDelayed`
- `deliveryCompleted`
- `routePlansCreated`
- `backlogCount`
- `workforce.driverCapacity`
- `workforce.usedCapacity`
- `workforce.bottleneckRole`

Existing fields remain compatible.

## Runtime Event Types

Archive-Logistics exposes the following event types for ArchiveOS:

- `SHIPMENT_CREATED`
- `ROUTE_ASSIGNED`
- `ROUTE_COST_CALCULATED`
- `TRUCK_DISPATCHED`
- `DELIVERY_DELAYED`
- `DELIVERY_COMPLETED`
- `LOGISTICS_COST_CONFIRMED`
- `LEDGER_EVENT_PUBLISHED`
- `WORKFORCE_ALLOCATION_ASSIGNED`
- `WORKDAY_COMPLETED`
- `CAPACITY_SHORTAGE_DETECTED`
- `LOGISTICS_BACKLOG_INCREASED`

## Safety Rules

- Runtime events are read-only projections.
- `eventType`, enum, API path, ID, and correlation fields are not translated.
- `orderId`, `correlationId`, `simulationRunId`, and `settlementCycleId` are preserved when Nexus provides them.
- Metadata exposes synthetic IDs and synthetic categories only.
- `originCode` and `destinationCode` are not exposed as address-like runtime metadata; `originType` and `destinationType` are used instead.
- Failed publish/callback state is exposed with `warning` or `critical` severity.
