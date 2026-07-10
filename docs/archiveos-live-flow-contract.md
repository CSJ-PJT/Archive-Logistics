# ArchiveOS Live Flow Contract

## 목적

ArchiveOS Live Flow / Operational Twin은 Archive-Logistics의 runtime event, outbox,
workforce, capacity, productivity, economy 상태를 read-only로 수집한다.

ArchiveOS가 꺼져 있어도 Archive-Logistics는 독립적으로 동작한다.
이 계약은 pull 기반 조회이며 외부 write를 요구하지 않는다.

## Read-only 수집 API

```http
GET /api/runtime-events/recent?limit=100
GET /api/runtime-events/correlation/{correlationId}
GET /api/runtime-events/entity/{entityId}
GET /api/workforce/summary
GET /api/productivity/summary
GET /api/capacity/summary
GET /api/operations/summary
GET /api/logistics-economy/summary
GET /api/outbox/summary
```

## Runtime Mapping

| Source table | Runtime domain | Entity type | Status mapping |
| --- | --- | --- | --- |
| `nexus_logistics_event` | `logistics` | `nexus_logistics_event` | `RECEIVED -> moving`, `PROCESSED -> completed`, `FAILED -> failed` |
| `route_plan` | `logistics` | `route_plan` | delayed route -> `delayed`, normal route -> `completed` |
| `logistics_outbox_event` | `logistics` | outbox aggregate type | `PENDING -> moving`, `RETRY -> waiting`, `PUBLISHED -> settled`, `FAILED -> failed`, `SKIPPED -> unavailable` |

## Operational Twin 원칙

- fake random animation data를 만들지 않는다.
- API 응답은 persisted synthetic runtime data에서만 만든다.
- `sourceService`, `eventType`, `status`, `correlationId`, `entityId`를 일관되게 제공한다.
- metadata에는 synthetic ID만 제공한다.
- 실제 개인정보/금융정보/배송주소/직원정보는 저장하거나 노출하지 않는다.
