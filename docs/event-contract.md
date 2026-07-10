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

Optional Market metadata fields:

- `orderId`
- `customerId`
- `customerType`
- `productType`
- `orderAmount`
- `totalAmount`
- `riskLevel`
- `expressOrder`
- `riskTag`
- `vipCustomer`
- `simulationRunId`
- `settlementCycleId`
- `correlationId`
- `causationId`
- `hopCount`
- `maxHop`
- `marketPriority`

## Ledger Output Events

Archive-Logistics creates Ledger-compatible logistics cost events through the outbox.

Default publish contract:

```http
POST http://localhost:18080/api/events/logistics/bulk
Content-Type: application/json
```

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
        "orderId": "ORD-2026-0001",
        "customerId": "CUST-0001",
        "customerType": "VIP_CUSTOMER",
        "productType": "battery-module",
        "orderAmount": 1500,
        "totalAmount": 1800,
        "totalCost": 93420,
        "currency": "KRW",
        "requiresApproval": false,
        "sourceChain": "Archive-Market -> Archive-Nexus -> Archive-Logitics",
        "riskLevel": 1,
        "marketPriority": "HIGH",
        "expressOrder": false,
        "simulationRunId": "SIM-20260710-001",
        "settlementCycleId": "CYCLE-20260710",
        "correlationId": "CORR-001",
        "causationId": "CAUSE-001",
        "hopCount": 1,
        "maxHop": 5
      }
    }
  ]
}
```

Supported output event types include:

- `LOGISTICS_COST_CONFIRMED`
- `URGENT_DELIVERY_COST_CONFIRMED`
- `DELAY_PENALTY_CONFIRMED`
- `ROUTE_DEVIATION_COST_CONFIRMED`
- `COLD_CHAIN_RISK_COST_CONFIRMED`

Compatibility mode may publish `LOGISTICS_DISPATCHED` shaped payloads when `ARCHIVE_LEDGER_V1_COMPAT` is enabled.

Ledger uses these events to create finance transactions, ledger entries, daily settlement targets, and reconciliation counts. Archive-Logistics does not create Ledger settlement or reconciliation rows directly.

## Nexus Daily Settlement Callback

After Logistics cost events are published to Ledger, Archive-Logistics can send a daily manufacturing compensation callback to Archive-Nexus.

```http
POST http://localhost:8080/api/logistics/settlements/daily
Content-Type: application/json
```

```json
{
  "settlementId": "LGS-SETTLE-20260709-FAC-A",
  "idempotencyKey": "LOGISTICS:DAILY:2026-07-09:FAC-A",
  "source": "Archive-Logistics",
  "schemaVersion": 1,
  "settlementDate": "2026-07-09",
  "factoryId": "FAC-A",
  "currency": "KRW",
  "totalShipments": 120,
  "delayedShipments": 18,
  "heldShipments": 3,
  "totalQuantity": 8400,
  "totalLogisticsCost": 4200000,
  "manufacturingImpactCost": 1260000,
  "onTimeRate": 0.8500,
  "evidence": {
    "basis": "published synthetic daily route cost summary",
    "settlementBasis": "logistics_outbox_event.status=PUBLISHED",
    "manufacturingShareRate": 0.3000
  },
  "payload": {
    "syntheticData": true,
    "settlementRole": "Manufacturing compensation callback from Logistics to Nexus"
  },
  "occurredAt": "2026-07-09T10:00:00Z"
}
```

Nexus settlement idempotency:

```text
LOGISTICS:DAILY:{settlementDate}:{factoryId}
```

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

## Market Origin Metadata Traceability

`Archive-Logistics`는 `Archive-Market`와 직접 연동하지 않고,
`Archive-Nexus` 경유로 전달된 주문/고객/정산 메타데이터를 보존합니다.

추가 문서:

- [Market Origin Metadata](./market-origin-logistics-metadata.md)
