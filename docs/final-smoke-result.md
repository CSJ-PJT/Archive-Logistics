# Final Smoke Result (Archive-Logistics)

Date: 2026-07-09

## Scope

- Archive-Logistics synthetic logistics route/cost pipeline
- Outbox + Spring Batch publisher + Ledger publish retry policy
- Economy extension (revenue/cost/settlement summaries)
- `/api/routes/summary` robustness fix and verification

## `/api/routes/summary` 500 status

Issue:

- Previous `JPQL` implementation could fail with `could not determine data type` when filters were mixed (`factoryId`, `date`).

Fix:

- Route summary now uses repository query branch selection by filter combination:
  - no filter
  - factoryId only
  - date only
  - factoryId + date
- This removes nullable parameters from query binding in the problematic paths.

Verified endpoints:

- `GET /api/routes/summary`
- `GET /api/routes/summary?factoryId=FAC-A`
- `GET /api/routes/summary?date=2026-01-15`
- `GET /api/routes/summary?factoryId=FAC-A&date=2026-01-15`

All returned HTTP `200`.

## Smoke Check

Executed against local service (port `8092`):

```bash
curl.exe http://localhost:8092/actuator/health
curl.exe http://localhost:8092/api/operations/summary
curl.exe http://localhost:8092/api/routes/summary
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A"
curl.exe "http://localhost:8092/api/routes/summary?date=2026-01-15"
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A&date=2026-01-15"
curl.exe http://localhost:8092/api/outbox/summary
curl.exe -X POST "http://localhost:8092/api/simulations/shipments?count=100"
curl.exe -X POST "http://localhost:8092/api/simulations/shipments?count=1000"
curl.exe -X POST "http://localhost:8092/api/outbox/publish"
curl.exe -X POST "http://localhost:8092/api/logistics-settlements/daily/run?date=2026-01-15"
curl.exe http://localhost:8092/api/logistics-economy/summary
curl.exe http://localhost:8092/api/operations/summary
```

Observed result:

- `GET /actuator/health` : `UP`
- All summary APIs above returned HTTP `200`
- Simulation APIs returned HTTP `200`
- `/api/outbox/publish` completed successfully according to current publish mode
- `/api/logistics-economy/summary` returned correctly

## Operational Notes

- When Ledger is disabled, publish is handled by `DRY_RUN/SKIPPED` behavior and does not stop service.
- Retry metadata (`retry_count`, `last_error`, `next_retry_at`, `maxHop` guard) is recorded for publish failures.
- Economy result is included in `/api/operations/summary` and can be traced via economy endpoints.

## Remaining Issues

- No critical blockers observed.
- Multi-process/local conflicts are avoided using environment profile and compose settings (PostgreSQL external port set to `5434`, app port `8092`).
