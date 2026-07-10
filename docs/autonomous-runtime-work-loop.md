# Autonomous Runtime Work Loop

## Purpose

Archive-Logistics can run a bounded autonomous work loop so ArchiveOS Live Flow does not appear stalled while the service is healthy.
The loop produces Synthetic Runtime Data only.

## Behavior

On each tick:

1. Check outbox backlog.
2. If backlog is below `archive.runtime.max-backlog-per-tick`, create up to `archive.runtime.max-events-per-tick` synthetic Nexus shipment events.
3. Process the events through the normal Nexus ingestion, route calculation, cost calculation, economy, and outbox flow.
4. Run a workday capacity/productivity update for the current day.
5. Update in-memory runtime status.

If backlog is at or above the limit, the loop does not create new shipments and only updates the workday tick.

## API

```http
GET /api/runtime/status
```

## Configuration

```yaml
archive:
  runtime:
    autorun:
      enabled: true
    tick-interval: 30s
    initial-delay: 15s
    max-events-per-tick: 10
    max-backlog-per-tick: 50
    max-hop: 5
```

Base profile defaults to disabled. `local` enables autorun by default. `oci-lite` keeps autorun disabled and lowers suggested limits.

## Safety

- Same tick key produces the same `eventId` and `idempotencyKey`.
- Duplicate tick execution is handled by existing duplicate guards.
- A scheduler lock prevents concurrent tick execution in the same JVM.
- Per-tick event count is capped.
- Backlog guard prevents event explosion when Ledger/outbox processing is behind.
- GET summary APIs remain read-only.
- External writes still follow existing integration enabled settings.
