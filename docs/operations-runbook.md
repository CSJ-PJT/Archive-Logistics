# Operations Runbook

## First Checks

```powershell
curl.exe http://localhost:8092/
curl.exe http://localhost:8092/actuator/health
curl.exe http://localhost:8092/api/operations/summary
```

Healthy baseline:

- operations dashboard loads
- actuator status is `UP`
- operations status is `HEALTHY`
- `failedEvents` is low or zero
- `outbox.failed` is low or zero
- Ledger status matches the intended environment

## If Route Summary Fails

Run:

```powershell
curl.exe http://localhost:8092/api/routes/summary
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A"
curl.exe "http://localhost:8092/api/routes/summary?date=2026-01-15"
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A&date=2026-01-15"
```

Expected result is HTTP 200 for all combinations. Empty date filters should return zero aggregation.

If a failure occurs:

- check application logs for SQL/JDBC errors
- confirm PostgreSQL connection
- confirm Flyway migrations ran
- confirm route tables exist and schema matches entities

## If Outbox Pending Grows

Check:

```powershell
curl.exe http://localhost:8092/api/outbox/summary
curl.exe http://localhost:8092/api/operations/summary
```

Possible causes:

- Ledger disabled intentionally
- publisher scheduler disabled
- Ledger endpoint unavailable
- repeated publish failures causing RETRY or FAILED

Actions:

- if local/dev: verify `ARCHIVE_LEDGER_ENABLED=true` unless a dry-run drill is intended
- use `POST /api/outbox/publish` or `POST /api/batch/outbox-publish/run` for explicit publish
- check `ARCHIVE_OUTBOX_SCHEDULER_ENABLED=true` only when automatic publish is intended
- check `ARCHIVE_LEDGER_BULK_ENDPOINT=/api/events/logistics/bulk`
- check `ARCHIVE_LEDGER_CONTRACT_MODE=LOGISTICS_CONFIRMED_NATIVE`
- inspect `last_error` on outbox events
- run manual publish after dependency recovery

## If Ledger Publish Fails

Check configuration:

```yaml
archive:
  ledger:
    enabled: true
    base-url: http://localhost:18080
    bulk-endpoint: /api/events/logistics/bulk
    contract-mode: LOGISTICS_CONFIRMED_NATIVE
```

Then verify:

- Ledger service is reachable
- bulk endpoint contract matches configured `contract-mode`
- timeout is not too low for the environment
- `retry_count` is increasing as expected

## Ledger Publish Settings

Docker/local defaults:

```env
ARCHIVE_LEDGER_ENABLED=true
ARCHIVE_LEDGER_BASE_URL=http://host.docker.internal:18080
ARCHIVE_LEDGER_BULK_ENDPOINT=/api/events/logistics/bulk
ARCHIVE_LEDGER_CONTRACT_MODE=LOGISTICS_CONFIRMED_NATIVE
ARCHIVE_LEDGER_PUBLISH_TIMEOUT_MS=30000
ARCHIVE_OUTBOX_SCHEDULER_ENABLED=false
ARCHIVE_OUTBOX_SCHEDULER_FIXED_DELAY_MS=30000
```

Run manual publish:

```powershell
curl.exe -X POST http://localhost:8092/api/outbox/publish
```

After successful Ledger ingestion, Ledger can run:

```powershell
curl.exe -X POST "http://localhost:18080/api/settlements/daily/run?date=YYYY-MM-DD"
curl.exe -X POST "http://localhost:18080/api/reconciliation/daily?date=YYYY-MM-DD"
curl.exe http://localhost:18080/api/reconciliation/summary
```

## If Nexus Daily Settlement Fails

Archive-Logistics can compensate Archive-Nexus from logistics costs that have already been published to Ledger.

Run:

```powershell
curl.exe -X POST "http://localhost:8092/api/settlements/nexus-daily/run?date=YYYY-MM-DD"
curl.exe http://localhost:8092/api/settlements/nexus-daily/summary
```

Expected behavior:

- only route plans with `logistics_outbox_event.status=PUBLISHED` are included
- `enabled=false` records `DRY_RUN`
- Nexus unavailable records `RETRY` or `FAILED`
- duplicate already-sent settlements are skipped safely

Check configuration:

```yaml
archive:
  nexus-settlement:
    enabled: true
    base-url: http://localhost:8080
    daily-endpoint: /api/logistics/settlements/daily
    manufacturing-share-rate: 0.3000
```

## If Nexus Events Fail

Check:

- validation errors in response
- unknown synthetic route
- duplicate `eventId` or `idempotencyKey`
- audit log entries for failure action

Unknown routes should be treated as data contract problems, not map API failures, because Archive-Logistics uses only synthetic route codes.

## Low Memory Operation

Use `oci-lite` profile and reduce:

- JVM heap
- Hikari pool
- batch chunk size
- scheduler frequency
- retention period

See [oci-lite-profile.md](oci-lite-profile.md).
