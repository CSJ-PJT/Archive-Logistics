# Operations Summary Contract

ArchiveOS는 `/api/operations/summary`를 read-only로 호출해 Archive-Logistics의 운영 상태를 수집한다.

## API

```http
GET /api/operations/summary
```

## ArchiveOS용 주요 필드

- `serviceName`: `Archive-Logistics`
- `serviceRole`: `Synthetic Logistics Event Backend`
- `status`: `HEALTHY` 또는 `DEGRADED`
- `latestEventAt`: 최근 persisted runtime event 시각
- `degradedReason`: 장애 또는 병목 사유
- `liveFlowAvailable`: runtime-events API 사용 가능 여부
- `outbox.pending`
- `outbox.published`
- `outbox.failed`
- `outbox.retry`
- `economy.totalRevenue`
- `economy.totalCost`
- `economy.totalProfit`
- `workforce.capacityEvents`
- `workforce.workloadEvents`
- `workforce.backlogEvents`
- `workforce.shortageEvents`

기존 필드는 제거하지 않는다. ArchiveOS 신규 화면은 위 필드를 우선 사용하고,
기존 대시보드와 smoke test는 기존 `service`, `status`, `outbox`, `economy`, `workforce` 구조를 계속 사용할 수 있다.
