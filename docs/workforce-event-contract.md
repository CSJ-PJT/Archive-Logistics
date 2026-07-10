# Workforce Event Contract

## 공통 이벤트 타입

- `WORKFORCE_ALLOCATION_ASSIGNED`
- `WORKDAY_STARTED`
- `WORKDAY_COMPLETED`
- `PRODUCTIVITY_REPORTED`
- `CAPACITY_SHORTAGE_DETECTED`
- `BACKLOG_INCREASED`
- `BOTTLENECK_DETECTED`

현재 MVP에서는 별도 외부 이벤트 발행 대신 API와 DB row로 workforce 상태를 관리한다.
향후 ArchiveOS orchestrator가 동일 필드를 기반으로 event publish를 제어할 수 있다.

## Allocation API

```http
POST /api/workforce/allocations
Content-Type: application/json
```

```json
{
  "sourceService": "ArchiveOS",
  "targetService": "Archive-Logistics",
  "workDate": "2026-07-10",
  "workdayId": "WORKDAY-20260710",
  "roles": [
    {
      "roleType": "ROUTE_PLANNER",
      "allocatedHeadcount": 1,
      "capacityPerPersonPerDay": 30,
      "productivityScore": 1.0,
      "wagePerDay": 190000
    },
    {
      "roleType": "DELIVERY_DRIVER",
      "allocatedHeadcount": 8,
      "capacityPerPersonPerDay": 8,
      "productivityScore": 1.0,
      "wagePerDay": 220000
    }
  ],
  "simulationRunId": "SIM-20260710-001",
  "settlementCycleId": "CYCLE-20260710",
  "correlationId": "CORR-WF-001",
  "causationId": "CAUSE-OS-001",
  "hopCount": 0,
  "maxHop": 5,
  "reason": "Synthetic workforce allocation"
}
```

## Role Type

- `DISPATCH_PLANNER`
- `ROUTE_PLANNER`
- `DELIVERY_DRIVER`
- `DELAY_RESPONSE_OPERATOR`
- `COLD_CHAIN_HANDLER`
- `LOGISTICS_MANAGER`

## 중복/순환 방지

- 같은 `workdayId + roleType` allocation은 중복 저장하지 않는다.
- route/cost 생성은 기존 `eventId`/`idempotencyKey` duplicate guard를 따른다.
- Ledger publish는 기존 `hopCount > maxHop` 차단 규칙을 따른다.
- workforce 상태 계산은 shipment 이벤트를 새로 생성하지 않는다.
