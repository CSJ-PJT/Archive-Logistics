# Runtime Operations Runbook

## Health and Runtime Checks

```powershell
curl.exe http://localhost:8092/actuator/health
curl.exe http://localhost:8092/api/runtime/status
curl.exe http://localhost:8092/api/operations/summary
curl.exe "http://localhost:8092/api/runtime-events/recent?limit=20"
curl.exe http://localhost:8092/api/workforce/summary
curl.exe http://localhost:8092/api/productivity/summary
curl.exe http://localhost:8092/api/capacity/summary
```

## Autonomous Work

For `local`, the bounded scheduler can create synthetic Nexus-origin shipment work, calculate route/ETA/cost through normal ingestion, update a workday productivity result, and leave Ledger-compatible outbox work. It does not use random data, actual addresses, or external writes that are not enabled.

Configuration:

```yaml
archive:
  runtime:
    autorun:
      enabled: true
    tick-interval: 30s
    max-events-per-tick: 10
    max-backlog-per-tick: 50
```

The scheduler lock, tick-derived idempotency keys, per-tick limit, outbox backlog guard, and hop limit prevent an event loop from growing without bound.

## Backlog Response

1. Read `/api/runtime/status` for `backlogCount`, `oldestBacklogAgeSeconds`, and `degradedReason`.
2. Read `/api/outbox/summary` and inspect retry or failed events.
3. Confirm the Ledger integration configuration before manual publish.
4. Use the existing outbox retry/publish APIs only after the external integration is known to be available.

ArchiveOS collection is observational. An ArchiveOS outage is not a reason to stop logistics processing.
