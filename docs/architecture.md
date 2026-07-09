# Architecture

Archive-Logistics is the logistics event transformation service in the Archive Platform Ecosystem.

```text
Archive-Nexus -> Archive-Logistics -> Archive-Ledger -> Nexus Daily Settlement -> ArchiveOS
```

## Responsibility Boundary

Archive-Logistics owns only logistics event processing.

- Receive logistics-related Nexus events
- Enforce `eventId` / `idempotencyKey` idempotency
- Calculate deterministic synthetic route, ETA, risk, and cost
- Persist `route_plan` and `route_cost`
- Create Ledger-compatible logistics cost events
- Store publish targets in PostgreSQL outbox
- Publish outbox events through Spring Batch / service publisher
- Send Ledger native logistics bulk events to `/api/events/logistics/bulk`
- Create Nexus daily manufacturing compensation callbacks from Ledger-published logistics costs
- Provide operations, route summary, outbox summary, health, and audit visibility
- Serve a lightweight operations dashboard from `/` and `/dashboard.html`

Archive-Logistics does not own Ledger domain tables such as financial transactions, ledger entries, settlement batches, reconciliation results, or approval requests.

## Runtime Flow

1. Archive-Nexus sends `LOGISTICS_DISPATCHED` or related logistics events.
2. Archive-Logistics stores the received event in `nexus_logistics_event`.
3. The service checks duplicate `eventId` and `idempotencyKey`.
4. `SyntheticRouteCalculator` calculates route plan, ETA, risk, delay, deviation, and route cost.
5. Route and cost records are persisted.
6. A Ledger-compatible payload is stored in `logistics_outbox_event`.
7. A manual API call, explicit Spring Batch job, or opt-in scheduler publishes eligible outbox events.
8. Archive-Ledger receives logistics cost events and can create finance transactions for daily settlement and reconciliation.
9. The Nexus daily settlement batch calculates manufacturing compensation only from route costs whose Logistics outbox is already `PUBLISHED`.
10. Archive-Logistics sends an idempotent daily settlement callback to Archive-Nexus.
11. Publish attempts are recorded in `ledger_publish_attempt`.
12. Audit records are stored in `audit_log`.
13. The operations dashboard reads summary APIs and visualizes Nexus -> Logistics -> Outbox -> Ledger -> Nexus Settlement -> ArchiveOS flow.

## Failure Isolation

Ledger availability does not control Nexus event ingestion or route/cost calculation.

- Ledger disabled: publish returns DRY_RUN/SKIPPED without external request.
- Ledger unavailable: outbox records retry metadata and keeps the service alive.
- Scheduler disabled: pending outbox rows remain until manual publish, batch run, or scheduler enablement.
- Retry metadata: `retry_count`, `last_error`, `next_retry_at`.
- Final failure: records move to `FAILED` after max retry count.

## Synthetic Routing

No real map API, OSRM, GraphHopper, real address, real vehicle, real carrier, or user location data is used. Distance and risk are derived from internal synthetic matrices and deterministic hashes.

## Naming Compatibility

External service name is **Archive-Logistics**. Some internal source names, class names, artifact names, and historical event values may still contain `Archive-Logitics` or `logitics` for compatibility with the original repository and previously generated event contracts.
