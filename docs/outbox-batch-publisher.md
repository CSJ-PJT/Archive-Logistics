# Outbox Batch Publisher

## Purpose

Archive-Logistics uses a PostgreSQL outbox to isolate Ledger publishing from Nexus event ingestion and route/cost calculation.

The service should keep accepting events and calculating logistics costs even when Ledger is disabled, unavailable, or slow.

## Outbox Table

`logistics_outbox_event` stores publish targets.

Key fields:

- `event_id`
- `idempotency_key`
- `event_type`
- `aggregate_type`
- `aggregate_id`
- `payload`
- `status`
- `retry_count`
- `last_error`
- `next_retry_at`
- `published_at`

Supported statuses:

- `PENDING`
- `PUBLISHED`
- `FAILED`
- `RETRY`
- `SKIPPED`

## Publish Rules

Eligible records:

- `status = PENDING`, or
- `status = RETRY` and `next_retry_at <= now`

Default chunk size:

```yaml
archive:
  outbox:
    chunk-size: 50
    max-retry-count: 5
```

## Ledger Disabled Mode

When `archive.ledger.enabled=false`, publish does not call Ledger.

Expected behavior:

- API returns DRY_RUN/SKIPPED result
- publish attempt is recorded
- service remains healthy
- no external request is sent

Docker/local demo defaults set `archive.ledger.enabled=true` so manual publish and `outboxPublishJob` can send real events to Ledger. Disable it only for fault-isolation drills or when Ledger is intentionally offline.

## Retry Behavior

On publish failure:

- increment `retry_count`
- store `last_error`
- calculate `next_retry_at`
- set `RETRY` until max retry count
- set `FAILED` after max retry count is reached

## Spring Batch and Scheduler

`outboxPublishJob` processes outbox rows in chunks. Docker/local demo configuration keeps the scheduler disabled by default; manual publish and explicit batch runs are still real Ledger sends when `archive.ledger.enabled=true`.

Scheduler settings:

```yaml
archive:
  outbox:
    scheduler:
      enabled: false
      fixed-delay-ms: 30000
```

Enable the scheduler only when automatic external delivery is intended:

```env
ARCHIVE_OUTBOX_SCHEDULER_ENABLED=true
```

Default Ledger publish contract:

```yaml
archive:
  ledger:
    enabled: true
    base-url: http://localhost:18080
    bulk-endpoint: /api/events/logistics/bulk
    contract-mode: LOGISTICS_CONFIRMED_NATIVE
```

Docker uses `http://host.docker.internal:18080` by default so the Logistics container can reach a Ledger process running on the host.

## Manual Publish

```powershell
curl.exe -X POST http://localhost:8092/api/outbox/publish
```

## Retry Failed

```powershell
curl.exe -X POST http://localhost:8092/api/outbox/retry-failed
```
