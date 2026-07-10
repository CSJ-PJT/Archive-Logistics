# Logistics Runtime Event Contract

## Common Response

```json
{
  "eventId": "ROUTE-20260710-abc123:TRUCK_DISPATCHED",
  "sourceService": "Archive-Logistics",
  "domain": "logistics",
  "eventType": "TRUCK_DISPATCHED",
  "entityType": "shipment",
  "entityId": "SHIP-000123",
  "correlationId": "CORR-000123",
  "causationId": "evt-nexus-000123",
  "status": "moving",
  "severity": "info",
  "displayLabel": "Synthetic truck dispatched",
  "occurredAt": "2026-07-10T10:00:00",
  "metadata": {
    "routePlanId": "ROUTE-20260710-abc123",
    "shipmentId": "SHIP-000123",
    "orderId": "ORD-000123",
    "destinationType": "DISTRIBUTION_CENTER"
  }
}
```

## Projection Mapping

| Source | Runtime event | Entity type | Status | Severity |
| --- | --- | --- | --- | --- |
| `nexus_logistics_event` | `SHIPMENT_CREATED` | `nexus_logistics_event` | `moving/completed/failed` | `info/critical` |
| `route_plan` | `ROUTE_ASSIGNED` | `route_plan` | `completed` | `normal` |
| `route_plan` | `TRUCK_DISPATCHED` | `shipment` | `moving` | `info` |
| `route_plan.delayed=true` | `DELIVERY_DELAYED` | `shipment` | `delayed` | `warning` |
| `route_plan.delayed=false` | `DELIVERY_COMPLETED` | `shipment` | `completed` | `normal` |
| `route_cost` | `ROUTE_COST_CALCULATED` | `route_cost` | `completed` | `info/warning` |
| `logistics_outbox_event` | `LOGISTICS_COST_CONFIRMED` | aggregate type | publish status | `info/warning/critical` |
| `logistics_outbox_event.PUBLISHED` | `LEDGER_EVENT_PUBLISHED` | aggregate type | `settled` | `info` |
| `logistics_workforce_allocation` | `WORKFORCE_ALLOCATION_ASSIGNED` | `workforce_allocation` | `completed` | `info` |
| `logistics_workday_result` | `WORKDAY_COMPLETED` | `workday` | `completed` | `info/warning` |

## Metadata Policy

Allowed metadata is limited to synthetic traceability fields:

- `shipmentId`
- `routePlanId`
- `orderId`
- `workdayId`
- `settlementCycleId`
- `originType`
- `destinationType`
- capacity and backlog counters

Forbidden metadata:

- real address
- real person name
- phone number
- vehicle plate number
- card number
- bank account number
- payment token
- secret, token, password, webhook, private key

`Archive-Market` metadata is optional and is preserved only when Nexus forwards it.
