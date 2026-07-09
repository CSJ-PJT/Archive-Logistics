# Logistics Daily Settlement

Archive-Logistics는 route/cost 결과를 집계해 Nexus 정산 대상 금액을 계산하고,
outbox 를 통해 결제·정산 이벤트를 발행합니다.

## 요약

`/api/logistics-settlements/daily/run?date=YYYY-MM-DD` 호출 시:

- 날짜/Factory별 route 집계를 계산
- 일일 정산 레코드 생성/갱신
- `LOGISTICS_DAILY_SETTLEMENT_FEE_EARNED` 이벤트를 outbox 등록(동일 키 중복 차단)

## 핵심 계산

- `totalDeliveryFee = Σ route_cost.total_cost`
- `totalSurcharge = Σ urgent + cold + deviation + penalty surcharge`
- `ledgerFee = 고정 수수료(기본값 12,000 + 5,000)`
- `netProfit = totalDeliveryFee + totalSurcharge - totalCost - ledgerFee`
- `totalRoutes`는 settlement 집계 대상 route count

## API

- `POST /api/logistics-settlements/daily/run?date=YYYY-MM-DD`
- `GET /api/logistics-settlements`
- `GET /api/logistics-settlements/{settlementId}`
- `GET /api/logistics-settlements/summary`

요청 파라미터:

- `date`: yyyy-MM-dd
- `factoryId`: 선택값

## Outbox/정산 연동

각 settlement는 outbox 상태를 통해 Ledger publish와 분리되어 운영됩니다.
Ledger 장애 시 재시도 정책이 유지되며, settlement는 idempotency key로 중복 안전합니다.

## 모니터링

- `GET /api/logistics-settlements/summary`
- `GET /api/logistics-economy/summary`
- `GET /api/operations/summary`
