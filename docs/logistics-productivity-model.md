# Logistics Productivity Model

## 개요

Archive-Logistics의 productivity는 synthetic workforce role별 capacity와 실제 logistics workload를 비교해 계산한다.
실제 직원, 차량, 배송 주소, 전화번호는 사용하지 않는다.

## 핵심 지표

- `totalCapacity`: role별 effective capacity 합계
- `usedCapacity`: route/delivery/delay 대응에 사용된 capacity
- `remainingCapacity`: 남은 capacity
- `shipmentsRequested`: 해당 일자 route workload
- `shipmentsDispatched`: 배차 처리된 shipment 수
- `shipmentsDelayed`: capacity 부족으로 지연 처리된 shipment 수
- `routePlansCreated`: route planner capacity 내에서 생성 가능한 route plan 수
- `deliveryCompleted`: driver capacity 내 완료 가능한 배송 수
- `backlogCount`: capacity 초과분
- `payrollCost`: role별 synthetic wage 합계
- `bottleneckRole`: 병목 role

## 계산 방식

```text
effectiveCapacity = allocatedHeadcount * capacityPerPersonPerDay * productivityScore
shipmentsProcessed = min(requestedShipments, effectiveDriverCapacity)
backlogCount = max(0, workloadEvents - totalCapacity)
remainingCapacity = max(0, totalCapacity - usedCapacity)
```

Priority가 `HIGH` 또는 `CRITICAL`인 배송은 route/cost 계산에서 긴급 surcharge를 유지한다.
Workforce 모델에서는 병목 상황에서 driver/route/delay/cold-chain capacity가 부족한 role을 `bottleneckRole`로 노출한다.

## Economy 반영

workday run 시 다음 synthetic cost가 economy에 반영된다.

- `LOGISTICS_WORKFORCE_PAYROLL_COST_INCURRED`
- `LOGISTICS_BACKLOG_COST_INCURRED`
- `DELIVERY_DELAY_OPERATION_COST_INCURRED`

`ARCHIVE_ECONOMY_ENABLED=false`이면 비용 이벤트 저장은 비활성화되고, workforce summary 계산만 수행한다.
