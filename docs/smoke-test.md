# Smoke Test

## Prerequisite

Start the service:

```powershell
docker compose up --build
```

Or run with local dependencies:

```powershell
.\gradlew.bat bootRun
```

## Health

```powershell
curl.exe http://localhost:8092/
curl.exe http://localhost:8092/actuator/health
curl.exe http://localhost:8092/api/health
```

Expected:

- HTTP 200
- dashboard HTML is returned for `/`
- actuator status `UP`
- API service name `Archive-Logistics`

## Generate Synthetic Shipments

```powershell
curl.exe -X POST "http://localhost:8092/api/simulations/shipments?count=100"
```

Expected:

- HTTP 200
- `processedCount` equals requested count when no duplicates exist
- outbox records are created

## Route Summary

```powershell
curl.exe http://localhost:8092/api/routes/summary
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A"
curl.exe "http://localhost:8092/api/routes/summary?date=2026-01-15"
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A&date=2026-01-15"
```

Expected:

- all return HTTP 200
- empty date filters return zero aggregation instead of 500

## Outbox

```powershell
curl.exe http://localhost:8092/api/outbox/summary
curl.exe -X POST http://localhost:8092/api/outbox/publish
```

Expected when Ledger is disabled:

- HTTP 200
- publish status DRY_RUN/SKIPPED
- no external Ledger request

## Operations Summary

```powershell
curl.exe http://localhost:8092/api/operations/summary
```

Check:

- `status`
- `receivedEvents`
- `processedEvents`
- `failedEvents`
- `outbox.pending`
- `outbox.retry`
- `outbox.failed`
- `ledger.enabled`
- `ledger.status`

## Build-Level Verification

```powershell
.\gradlew.bat test --no-daemon --console=plain
.\gradlew.bat build --no-daemon --console=plain
docker compose config --quiet
```
