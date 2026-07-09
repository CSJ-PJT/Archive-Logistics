# Demo Scenario

1. Start services.

```powershell
docker compose up --build
```

2. Check health.

```powershell
curl.exe http://localhost:8092/actuator/health
```

3. Generate synthetic shipments.

```powershell
curl.exe -X POST "http://localhost:8092/api/simulations/shipments?count=100"
```

4. Query routes and costs.

```powershell
curl.exe "http://localhost:8092/api/routes/plans?page=0&size=20"
curl.exe "http://localhost:8092/api/routes/costs?page=0&size=20"
```

5. Check outbox summary.

```powershell
curl.exe http://localhost:8092/api/outbox/summary
```

6. Publish with Ledger disabled.

```powershell
curl.exe -X POST http://localhost:8092/api/outbox/publish
```

The response is a dry-run result and no external Ledger request is sent.

7. Check operations summary.

```powershell
curl.exe http://localhost:8092/api/operations/summary
```

8. Verify summary variants (요약 API 스모크).

```powershell
curl.exe "http://localhost:8092/api/routes/summary"
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A"
curl.exe "http://localhost:8092/api/routes/summary?date=2026-01-15"
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A&date=2026-01-15"
```

All responses should return HTTP 200 and include `routePlans` aggregation.

