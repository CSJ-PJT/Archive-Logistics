# Logistics Delay and Capacity Contract

## Purpose

ArchiveOS uses this contract to understand whether Logistics delay is caused by route risk, outbox backlog, or synthetic workforce capacity.

All values are synthetic runtime data.

## Delay Signals

`DELIVERY_DELAYED` is emitted when a route plan has `delayed=true`.

Runtime fields:

- `status`: `delayed`
- `severity`: `warning`
- `entityType`: `shipment`
- `entityId`: shipment ID
- `metadata.routePlanId`
- `metadata.destinationType`
- `metadata.delayed=true`

## Capacity Signals

`WORKDAY_COMPLETED` summarizes the latest workday result.

`CAPACITY_SHORTAGE_DETECTED` is emitted when `shortageEvents > 0`.

`LOGISTICS_BACKLOG_INCREASED` is emitted when `backlogEvents > 0`.

Runtime metadata:

- `shipmentsRequested`
- `shipmentsDispatched`
- `deliveryCompleted`
- `shipmentsDelayed`
- `bottleneckRole`
- `totalCapacity`
- `usedCapacity`
- `count`

## Operations Summary Fields

ArchiveOS can read capacity state from `GET /api/operations/summary`:

- `shipmentsRequested`
- `shipmentsDispatched`
- `shipmentsDelayed`
- `deliveryCompleted`
- `routePlansCreated`
- `backlogCount`
- `workforce.driverCapacity`
- `workforce.usedCapacity`
- `workforce.bottleneckRole`

## Severity Rules

| Condition | Severity |
| --- | --- |
| normal dispatch/completion | `info` or `normal` |
| delayed shipment | `warning` |
| backlog increased | `warning` |
| capacity shortage | `critical` |
| outbox retry/skipped | `warning` |
| outbox failed | `critical` |

## Integration Boundary

ArchiveOS does not write to Logistics through these APIs.
Workforce allocation and workday execution remain separate operational APIs and still follow existing safe-mode and integration settings.
