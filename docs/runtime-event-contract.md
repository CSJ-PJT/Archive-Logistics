# Runtime Event Contract

ArchiveOS Live Flow는 Archive-Logistics의 runtime 상태를 read-only API로 수집한다.
이 API는 실제 배송/개인/금융 데이터를 만들지 않으며 기존 persisted synthetic runtime data만 노출한다.

## API

```http
GET /api/runtime-events/recent?limit=100
GET /api/runtime-events/correlation/{correlationId}
GET /api/runtime-events/entity/{entityId}
```

## 공통 응답 필드

```json
{
  "eventId": "evt-logitics-20260710-001",
  "sourceService": "Archive-Logistics",
  "domain": "logistics",
  "eventType": "LOGISTICS_COST_CONFIRMED",
  "entityType": "ROUTE_PLAN",
  "entityId": "ROUTE-20260710-001",
  "correlationId": "CORR-001",
  "causationId": "CAUSE-001",
  "status": "moving",
  "severity": "info",
  "displayLabel": "Ledger publish outbox event",
  "occurredAt": "2026-07-10T10:00:00",
  "metadata": {
    "routePlanId": "ROUTE-20260710-001",
    "shipmentId": "SHIP-001",
    "orderId": "ORD-001"
  }
}
```

## Status

- `created`
- `moving`
- `waiting`
- `approval_required`
- `approved`
- `rejected`
- `delayed`
- `failed`
- `completed`
- `settled`
- `unavailable`

## Metadata 제한

metadata에는 synthetic ID와 synthetic code만 포함한다.

금지:

- 실제 이름
- 전화번호
- 주소
- 카드번호
- 계좌번호
- 실제 결제 토큰
- secret/token/password/webhook/private key
