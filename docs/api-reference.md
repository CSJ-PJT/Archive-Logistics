# API Reference

Base URL: `http://localhost:8092`

## Health

```http
GET /actuator/health
```

Dashboard: `GET /` , `GET /dashboard.html`

## Operations

- `GET /api/operations/summary`
- `GET /api/routes/summary`
- `GET /api/routes/summary?factoryId=FAC-A`
- `GET /api/routes/summary?date=2026-01-15`
- `GET /api/routes/summary?factoryId=FAC-A&date=2026-01-15`
- `GET /api/outbox/summary`
- `GET /api/logistics-economy/summary`

## Routes

- `GET /api/routes/plans?page=0&size=20`
- `GET /api/routes/plans/{routePlanId}`
- `GET /api/routes/costs?page=0&size=20`
- `GET /api/routes/costs/{routePlanId}`

Pagination 기본 50, 최대 500.

## Nexus Input

```http
POST /api/events/nexus
POST /api/events/nexus/bulk
```

## Simulation

```http
POST /api/simulations/shipments?count=100
POST /api/simulations/shipments?count=1000
POST /api/simulations/shipments?count=10000
```

## Outbox

```http
GET /api/outbox/events
GET /api/outbox/events/{eventId}
GET /api/outbox/summary
POST /api/outbox/publish
POST /api/outbox/retry-failed
```

## Logistics Settlements (Economy)

```http
POST /api/logistics-settlements/daily/run?date=YYYY-MM-DD
GET /api/logistics-settlements
GET /api/logistics-settlements/{settlementId}
GET /api/logistics-settlements/summary?date=YYYY-MM-DD
```

## Logistics Economy

```http
GET /api/logistics-economy/summary
GET /api/logistics-economy/revenue-events
GET /api/logistics-economy/cost-events
GET /api/logistics-economy/profit-snapshots
```

## Batch

```http
POST /api/batch/outbox-publish/run
POST /api/batch/nexus-daily-settlement/run
GET /api/batch/jobs
GET /api/batch/jobs/{executionId}
```

## Legacy Nexus Daily Settlement

```http
POST /api/settlements/nexus-daily/run
GET /api/settlements/nexus-daily
GET /api/settlements/nexus-daily/summary
GET /api/settlements/nexus-daily/{settlementId}
```

Legacy API는 기존 Archive-Nexus 정산 흐름 호환성 유지 목적입니다.
