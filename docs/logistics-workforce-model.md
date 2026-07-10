# Logistics Operational Workforce Model

## 목적

`Archive-Logistics`는 synthetic workforce 배정에 따라 일별 처리 capacity,
backlog, bottleneck, productivity, synthetic labor cost를 산출한다.

실제 직원 이름, 급여, 개인정보는 사용하지 않는다. 모든 값은 Synthetic Data / Demo Data다.

## 담당 workforce role

- `DISPATCH_PLANNER`: 배차/출고 이벤트 정리
- `ROUTE_PLANNER`: route plan 생성
- `DELIVERY_DRIVER`: 배송 처리
- `DELAY_RESPONSE_OPERATOR`: 지연/우회 대응
- `COLD_CHAIN_HANDLER`: cold-chain risk 대응
- `LOGISTICS_MANAGER`: 전체 logistics 운영 조정

## 설정

기본값은 `archive.workforce.enabled=false`다.
disabled 상태에서도 baseline capacity로 요약을 계산하므로 기존 이벤트 처리 흐름은 유지된다.

주요 환경변수:

- `ARCHIVE_WORKFORCE_ENABLED`
- `ARCHIVE_WORKFORCE_BASELINE_DISPATCHERS`
- `ARCHIVE_WORKFORCE_BASELINE_DRIVERS`
- `ARCHIVE_WORKFORCE_BASELINE_DELAY_RESPONDERS`
- `ARCHIVE_WORKFORCE_DISPATCHER_DAILY_CAPACITY`
- `ARCHIVE_WORKFORCE_DRIVER_DAILY_CAPACITY`
- `ARCHIVE_WORKFORCE_DELAY_RESPONDER_DAILY_CAPACITY`

## API

```http
GET /api/workforce/summary
GET /api/productivity/summary
GET /api/capacity/summary
POST /api/workforce/allocations
POST /api/workforce/workday/run?date=YYYY-MM-DD
```

## Allocation 예시

```json
{
  "sourceService": "ArchiveOS",
  "workDate": "2026-07-10",
  "dispatchers": 3,
  "drivers": 8,
  "delayResponders": 2,
  "simulationRunId": "SIM-20260710-001",
  "settlementCycleId": "CYCLE-20260710",
  "correlationId": "CORR-WORKFORCE-001",
  "causationId": "CAUSE-OS-001",
  "hopCount": 0,
  "maxHop": 5,
  "reason": "Synthetic workforce allocation for Logistics daily capacity"
}
```

## Workday 계산

role별 필드:

- `allocatedHeadcount`
- `capacityPerPersonPerDay`
- `productivityScore`
- `wagePerDay`
- `effectiveCapacity`
- `usedCapacity`
- `remainingCapacity`

workload:

- 해당 일자 route plan 수
- 해당 일자 delayed route 수
- 현재 `PENDING`/`RETRY` Outbox backlog

capacity:

- `allocatedHeadcount * capacityPerPersonPerDay * productivityScore`

결과:

- `processedEvents = min(workloadEvents, capacityEvents)`
- `backlogEvents = max(0, workloadEvents - capacityEvents)`
- `status = BOTTLENECK_DETECTED` 또는 `PRODUCTIVITY_REPORTED`
- `bottleneckType = DELIVERY_DRIVER`, `ROUTE_PLANNER`, `DELAY_RESPONSE_OPERATOR`, `COLD_CHAIN_HANDLER`, `OUTBOX_PUBLISH_CAPACITY`, `NONE`

## ArchiveOS 연동

ArchiveOS는 다음 endpoint를 읽어 전체 ecosystem workforce 관제에 사용할 수 있다.

- `/api/operations/summary`의 `workforce` 섹션
- `/api/workforce/summary`
- `/api/productivity/summary`
- `/api/capacity/summary`
## Summary read-only contract

The following APIs are read-only projections for ArchiveOS Workforce Overview.

```http
GET /api/workforce/summary
GET /api/productivity/summary
GET /api/capacity/summary
```

Rules:

- Do not seed default allocations during summary reads.
- Do not insert `logistics_workday_result` rows during summary reads.
- Do not run simulation or outbox publish during summary reads.
- Return HTTP 200 with default synthetic values when no allocation or workday result exists.
- Expose ArchiveOS-friendly aliases such as `serviceName`, `available`, `totalHeadcount`, `effectiveCapacity`, `backlogCount`, `bottleneckRole`, `payrollCost`, `latestEventAt`, and nullable `degradedReason`.
