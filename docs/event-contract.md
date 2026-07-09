# Event Contract

## Nexus Input Events

Supported Nexus event types:

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

## Ledger Output Events

Archive-Logistics creates Ledger-compatible logistics cost events through the outbox.

Supported output event types include:

- `LOGISTICS_COST_CONFIRMED`
- `URGENT_DELIVERY_COST_CONFIRMED`
- `DELAY_PENALTY_CONFIRMED`
- `ROUTE_DEVIATION_COST_CONFIRMED`
- `COLD_CHAIN_RISK_COST_CONFIRMED`

Compatibility mode may publish `LOGISTICS_DISPATCHED` shaped payloads when `ARCHIVE_LEDGER_V1_COMPAT` is enabled.

## Idempotency

Nexus input idempotency:

```text
NEXUS:{eventType}:{originCode}:{shipmentId}
```

Ledger output idempotency:

```text
LOGISTICS:{ledgerEventType}:{routePlanId}
```

Duplicate policy:

- Same `eventId` or same `idempotencyKey` returns the existing processing result.
- No duplicate `route_plan`, `route_cost`, or `logistics_outbox_event` is created.
- Duplicate receipt is recorded in `audit_log`.

## Error Policy

- Unknown route: HTTP 400 style response, failed event stored, audit log recorded.
- Validation error: HTTP 400 with structured field errors.
- Duplicate event: HTTP 200 with duplicate result, no new route/cost/outbox.
- Ledger publish failure: outbox status and retry metadata are updated; API process does not crash.

## Data Policy

Events use synthetic factory, destination, route, and vendor codes only. Real addresses, real drivers, real vehicles, real carriers, payment cards, accounts, user locations, and personal data are not generated or stored.

