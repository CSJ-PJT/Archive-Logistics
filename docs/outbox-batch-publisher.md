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

## Retry Behavior

On publish failure:

- increment `retry_count`
- store `last_error`
- calculate `next_retry_at`
- set `RETRY` until max retry count
- set `FAILED` after max retry count is reached

## Spring Batch

`outboxPublishJob` processes outbox rows in chunks. Local profile keeps scheduler off by default to avoid accidental external delivery during development.

Scheduler should be enabled explicitly:

```yaml
archive:
  outbox:
    scheduler:
      enabled: true
      fixed-delay-ms: 30000
```

## Manual Publish

```powershell
curl.exe -X POST http://localhost:8092/api/outbox/publish
```

## Retry Failed

```powershell
curl.exe -X POST http://localhost:8092/api/outbox/retry-failed
```

