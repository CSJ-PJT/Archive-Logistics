# Operations Runbook

## First Checks

```powershell
curl.exe http://localhost:8092/actuator/health
curl.exe http://localhost:8092/api/operations/summary
```

Healthy baseline:

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

- if local/dev: verify DRY_RUN/SKIPPED is expected
- if Ledger should be enabled: check `ARCHIVE_LEDGER_ENABLED`
- inspect `last_error` on outbox events
- run manual publish after dependency recovery

## If Ledger Publish Fails

Check configuration:

```yaml
archive:
  ledger:
    enabled: true
    base-url: http://localhost:8093
    bulk-endpoint: /api/events/logistics/bulk
```

Then verify:

- Ledger service is reachable
- bulk endpoint contract matches configured `contract-mode`
- timeout is not too low for the environment
- `retry_count` is increasing as expected

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

