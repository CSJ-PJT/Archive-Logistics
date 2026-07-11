# Live Shipment Runtime and Balance

## Autonomous Shipment Flow

The bounded local/demo runtime tick uses the normal Nexus ingestion path. Each generated synthetic shipment follows this sequence:

```text
Nexus shipment request
  -> SHIPMENT_CREATED
  -> ROUTE_ASSIGNED
  -> ROUTE_COST_CALCULATED
  -> LOGISTICS_COST_CONFIRMED (Ledger outbox created)
  -> TRUCK_DISPATCHED
  -> DELIVERY_IN_TRANSIT
  -> DELIVERY_COMPLETED | DELIVERY_DELAYED
```

When a route is delayed by deterministic risk, exceeds the configured capacity budget, or exceeds its hop limit, it ends in `DELIVERY_DELAYED`. A delayed cold-chain route also emits `COLD_CHAIN_RISK_DETECTED`.

`shipment_runtime_event` stores each route/event type once using a deterministic idempotency key. The lifecycle service skips terminal routes, so a completed shipment cannot be completed again by a repeated tick.

## Capacity Rules

- tick work remains capped by `archive.runtime.max-events-per-tick`;
- priority is ordered `CRITICAL`, `HIGH`, then `NORMAL`;
- workday delivery capacity is the completion budget;
- over-budget work becomes delayed and contributes to the existing workforce backlog/capacity projections;
- existing outbox backlog and hop guards prevent event amplification.

## Balance Contract

ArchiveOS reads the `balance` section from operations/economy summaries. It is calculated read-only from persisted synthetic revenue events, cost events, route statuses, profit snapshots, and workforce results.

Every balance response includes `available`, `status`, `calculationScope`, and `calculatedAt`. When no persisted runtime/economy data exists, metric fields are `null`, `available=false`, and `status=NO_DATA` with a reason.

| Field | Source |
| --- | --- |
| `logisticsRevenue` | persisted logistics revenue events |
| `fuelCost`, `tollCost` | route cost events |
| `workforceCost` | payroll cost events |
| `delayPenaltyCost`, `coldChainCost` | risk and delay operation cost events |
| `ledgerFee` | Ledger settlement/reconciliation fee events |
| `operatingProfit`, `operatingMargin` | revenue minus total persisted cost |
| `cashBalance`, `negativeProfitStreak` | profit snapshots |
| shipment and ETA metrics | route lifecycle status and route plans |
| capacity metrics | persisted workforce workday result |

The operating-margin reference range is 3% to 10%; it is an ArchiveOS monitoring guideline, not a mutation rule. No GET summary endpoint creates data or changes financial state.

## Traceability and Safety

`orderId`, `customerType`, `productType`, `priority`, `correlationId`, `causationId`, `simulationRunId`, and `settlementCycleId` are preserved when supplied by Nexus. Runtime metadata uses synthetic `destinationType` and `syntheticHubId`; it never exposes actual addresses or personal data.

Ledger outbox creation remains part of the original ingestion transaction. ArchiveOS availability has no effect on shipment processing because ArchiveOS observes the local Runtime Mesh projection through pull APIs.
