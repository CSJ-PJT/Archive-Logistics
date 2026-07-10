# Market Origin Metadata 추적 (Archive-Logistics)

## 개요

`Archive-Logistics`는 `Archive-Market`와 직접 연동하지 않습니다.
메시지는 항상 `Archive-Nexus` 경유로 유입되며, Nexus payload에 포함되는 Market 메타데이터를 **선택적**으로 보존합니다.

기존 계약을 깨지 않기 위해:

- `Nexus` 입력 이벤트 스키마는 유지
- 실제 라우팅/비용 계산 로직은 기존 규칙을 유지
- Market metadata가 없는 요청은 기존 동작으로 그대로 처리

## 보존 메타데이터

Nexus payload에서 다음 값이 있으면 route/일반 outbox/ledger payload에 전달됩니다.

- `orderId`
- `customerId`
- `customerType`
- `productType`
- `orderAmount`
- `totalAmount`
- `riskLevel`
- `expressOrder`
- `riskTag`
- `vipCustomer`
- `simulationRunId`
- `settlementCycleId`
- `correlationId`
- `causationId`
- `hopCount`
- `maxHop`
- `marketPriority`

## 저장 위치

- `route_plan`:
  - `order_id`, `customer_id`, `customer_type`, `product_type`,
    `order_amount`, `total_amount`, `market_priority`, `risk_level`,
    `express_order`, `vip_customer`, `correlation_id`, `causation_id`,
    `simulation_run_id`, `settlement_cycle_id`, `hop_count`, `max_hop`
- Ledger ledger event payload:
  - `orderId`, `customerId`, `customerType`, `productType`, `orderAmount`, `totalAmount`,
    `riskLevel`, `expressOrder`, `simulationRunId`, `settlementCycleId`,
    `correlationId`, `causationId`, `hopCount`, `maxHop`, `sourceChain`

## 순환 안전장치

- `hopCount` 또는 누적 hop이 `maxHop`을 초과하면 Outbox publish 단계에서
  `SKIPPED` 처리되어 Ledger로 송신하지 않습니다.
- 동일 `eventId`/`idempotencyKey`는 중복 처리되어 route/cost/outbox를 재생성하지 않습니다.
- Ledger 응답/비용 이벤트가 다시 Logistics 비용을 발생시키는 이중 카운팅은 방지.

## DB 마이그레이션

- `V6__add_market_metadata_to_route_plan.sql`
  - `route_plan` 테이블에 Market 메타데이터 컬럼 추가

## 운영 지표 반영

`/api/operations/summary`는 다음 항목을 제공합니다.

- `marketOriginRoutes`
- `expressOrderRoutes`
- `vipCustomerRoutes`
- `highRiskCustomerRoutes`
