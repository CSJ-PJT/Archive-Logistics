# Logistics Economy Model

Archive-Logistics는 Ledger 연동과 독립된 순수 비용/수익 흐름을 별도 모델로 보유합니다.
목표는 물류 비용 산정의 회계성 단위를 명확히 하고, 이벤트 재발송 시에도 중복 정합성을 유지하는 것입니다.

## 이벤트 구분

### 수익 이벤트 (Revenue)

- `LOGISTICS_DELIVERY_FEE_EARNED`
- `LOGISTICS_URGENT_SURCHARGE_EARNED`
- `LOGISTICS_COLD_CHAIN_SURCHARGE_EARNED`
- `LOGISTICS_DAILY_SETTLEMENT_FEE_EARNED`
- `LOGISTICS_DELAY_PENALTY_RECHARGED`
- `LOGISTICS_ROUTE_DEVIATION_SURCHARGE_EARNED`

### 비용 이벤트 (Cost)

- `LOGISTICS_FUEL_COST_INCURRED`
- `LOGISTICS_TOLL_COST_INCURRED`
- `LOGISTICS_DELAY_PENALTY_COST_INCURRED`
- `LOGISTICS_COLD_CHAIN_RISK_COST_INCURRED`
- `LEDGER_SETTLEMENT_AGENCY_FEE_PAID`
- `LEDGER_RECONCILIATION_FEE_PAID`
- `LOGISTICS_OPERATION_COST_INCURRED`

## 생성 규칙(요약)

`route_cost` 생성 시:

- 기본 배송료 수익: `LOGISTICS_DELIVERY_FEE_EARNED`
- `priority=HIGH/CRITICAL` 시 `LOGISTICS_URGENT_SURCHARGE_EARNED`
- `requiresColdChain=true` 시 `LOGISTICS_COLD_CHAIN_SURCHARGE_EARNED`
- `delayed=true` 시 지연 비용 재발생 이벤트
- `deviated=true` 시 우회/편차 수익 이벤트

Ledger 실제 publish 실패/비활성에서는 비용 이벤트 생성 정책을 비활성 플래그로 분기합니다.

## 테이블

- `logistics_revenue_event`
- `logistics_cost_event`
- `logistics_daily_settlement`
- `logistics_profit_snapshot`

각 테이블은 `event_id`, `idempotency_key`로 중복 방지를 하며,
route/settlement 단위로 집계가 가능합니다.

## API

- `GET /api/logistics-economy/summary`
- `GET /api/logistics-economy/revenue-events`
- `GET /api/logistics-economy/cost-events`
- `GET /api/logistics-economy/profit-snapshots`

## 안전장치

- `simulationRunId`
- `settlementCycleId`
- `correlationId`
- `causationId`
- `hopCount`, `maxHop` (기본 10)
- 이미 처리한 `idempotencyKey`는 재생성/재발행을 차단
