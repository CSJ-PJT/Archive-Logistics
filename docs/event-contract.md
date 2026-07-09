# Event Contract

## Nexus Input

Supported event types:

- `LOGISTICS_DISPATCHED`
- `URGENT_DELIVERY_REQUESTED`
- `SHIPMENT_HOLD_RELEASED`
- `MATERIAL_TRANSFER_REQUESTED`
- `QUALITY_REPLACEMENT_SHIPMENT`

Required envelope fields:

- `eventId`
- `idempotencyKey`
- `source`
- `eventType`
- `occurredAt`
- `payload`

Required payload fields:

- `factoryId`
- `shipmentId`
- `originCode`
- `destinationCode`
- `priority`
- `itemType`
- `quantity`
- `requiresColdChain`

## Ledger Output

Outbox event types include:

- `LOGISTICS_COST_CONFIRMED`
- `URGENT_DELIVERY_COST_CONFIRMED`
- `DELAY_PENALTY_CONFIRMED`
- `ROUTE_DEVIATION_COST_CONFIRMED`
- `COLD_CHAIN_RISK_COST_CONFIRMED`

Idempotency key format:

```text
LOGITICS:{ledgerEventType}:{routePlanId}
```

Duplicate policy:

- Same `eventId` or same `idempotencyKey` returns the existing processing result.
- No duplicate `route_plan`, `route_cost`, or `logistics_outbox_event` is created.
- Duplicate receipt is recorded in `audit_log`.

Unknown route policy:

- API returns HTTP 400 style error.
- `nexus_logistics_event` is stored as `FAILED`.
- Failure reason and audit log are recorded.
