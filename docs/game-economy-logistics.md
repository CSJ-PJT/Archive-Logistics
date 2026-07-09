# Game Economy (Logistics)

본 항목은 ArchiveOS 내부 의사결정/시뮬레이션 계층에서 사용하는 개념 정리입니다.
실운영 로직은 `simulationRunId`, `settlementCycleId`, `hopCount`, `maxHop`를 사용해
루프와 중복을 제어합니다.

## 게임화 이벤트 설계

- 이벤트는 `simulationRunId` 단위로 배치 생성
- `idempotencyKey`를 중심으로 중복 실행 차단
- `hopCount`와 `maxHop`로 상호 호출 사이클(예: Ledger -> OS -> Ledger) 종료 조건 제어
- `causationId`로 원인 추적

## 게임 이벤트가 실제 처리에 미치는 영향

- 기본 이벤트 처리(route/cost 계산, outbox 생성)는 실운영 동일
- 게임/모의 이벤트는 `source=Archive-Logistics` 기반 synthetic payload로 한정
- 실제 결제/개인 정보/실차량 데이터는 미사용

## 루프 방지 규칙

- `hopCount > maxHop` 이면 publish 차단
- `idempotencyKey` 재수신 시 재생성 건너뜀
- Ledger 비용 이벤트가 다시 비용 이벤트를 재유발하지 않도록 명시적 타입/경로 분기

## 운영 관점

- ArchiveOS는 운영/대사 위험이 커지면 승인/개입 모드로 유도
- 운영자는 `/api/operations/summary`와 `/api/logistics-economy/summary` 기반으로 알림/조치
