# ArchiveOS Realtime Integration

## Integration Mode

Archive-Logistics currently supports ArchiveOS read-only pull through Runtime Mesh V1. The final ArchiveOS ingest payload and authentication contract has not been supplied to this repository, so Archive-Logistics does not create an incompatible `POST /api/live-flow/events/ingest` client or a new authentication header.

ArchiveOS can safely operate while Archive-Logistics is running, and Archive-Logistics remains fully functional when ArchiveOS is unavailable.

## Pull Sequence

```text
ArchiveOS -> GET /api/runtime/status
ArchiveOS -> GET /api/runtime-events/recent?limit=100
ArchiveOS -> store the first event cursor as the bootstrap high-water mark
ArchiveOS -> GET /api/runtime-events/recent?after={cursor}&limit=100
```

Use correlation and entity endpoints for drill-down after receiving a Live Flow item.

## Failure Isolation and Retry

- Runtime projection reads only persisted local data and never delete events.
- ArchiveOS read failures do not affect Nexus ingestion, route calculation, workforce workday processing, or Ledger outbox processing.
- Ledger delivery retains its existing DB outbox retry/backoff policy (`retry_count`, `last_error`, `next_retry_at`, and maximum retry count).
- ArchiveOS push remains disabled until its final ingest contract is explicitly configured. There is therefore no unbounded ArchiveOS retry queue or hidden external write.

## Event Safety

- Original `eventId` and `idempotencyKey` are retained when source data provides them.
- Projection-only lifecycle events use deterministic `RUNTIME:*` keys.
- `correlationId`, `causationId`, `simulationRunId`, `settlementCycleId`, and `workdayId` are propagated when available.
- `hopCount` and `maxHop` remain visible for loop protection. Existing publish paths enforce their hop guard before external delivery.
