# Route Summary Fix

## Issue

`GET /api/routes/summary` previously could fail with PostgreSQL JDBC error:

```text
could not determine data type
```

The failure was caused by nullable `date` and `factoryId` parameters being passed into JPQL conditions. PostgreSQL could not reliably infer the type of a null-bound parameter in that query shape.

## Impact

Affected endpoints:

- `GET /api/routes/summary`
- `GET /api/routes/summary?factoryId=FAC-A`
- `GET /api/routes/summary?date=2026-01-15`
- `GET /api/routes/summary?factoryId=FAC-A&date=2026-01-15`

The issue affected read-only operations but made operations dashboards and ArchiveOS-facing summary checks unstable.

## Resolution

The nullable conditional query path was removed. The service now selects the repository query path based on the provided filter combination.

- no filter
- `factoryId` only
- `date` only
- `factoryId + date`

This avoids ambiguous null parameter binding and keeps SQL parameter types explicit.

## Verification

Current smoke status:

- `GET /api/routes/summary` -> HTTP 200
- `GET /api/routes/summary?factoryId=FAC-A` -> HTTP 200
- `GET /api/routes/summary?date=2026-01-15` -> HTTP 200
- `GET /api/routes/summary?factoryId=FAC-A&date=2026-01-15` -> HTTP 200

If the requested date has no rows, the API returns zero-count aggregation instead of failing.

## Operational Check

```powershell
curl.exe http://localhost:8092/api/routes/summary
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A"
curl.exe "http://localhost:8092/api/routes/summary?date=2026-01-15"
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A&date=2026-01-15"
```

