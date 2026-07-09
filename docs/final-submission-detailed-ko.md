# Archive-Logistics 최종 제출 상세본 (한국어)

## 1. 제출 대상 및 범위
- 대상 저장소: `Archive-Logistics`
- 대상 브랜치: `main`
- 프로젝트 성격: Archive-Nexus 이벤트 수신 기반의 **Logistics Synthetic Backend**
- 제외 항목: Archive-OS / Archive-Ledger 본체 코드 미포함

## 2. 프로젝트 역할
Archive-Nexus에서 들어오는 출하 이벤트를 기반으로:
1. 경로 추정 및 ETA 산정
2. 연료비/통행료/긴급 배송료/지연·냉장 위험 비용 계산
3. 승인 임계치 계산(`requiresApproval`)
4. Ledger가 처리 가능한 비용 확정 이벤트 payload 생성
5. PostgreSQL Outbox 기반으로 안정적으로 publish 대상화

데이터는 **실제 지도 API/실제 배송·차량·주소·개인정보**를 사용하지 않고,  
deterministic synthetic matrix 및 synthetic id 규칙으로 운영됩니다.

## 3. 최종 검증 로그 (요약)
- `GET /actuator/health`  
  - 응답: `{"status":"UP"}` (정상)
- `GET /api/operations/summary`
  - service: Archive-Logistics, status: HEALTHY
  - profile: local
  - receivedEvents: 206, processedEvents: 206, failedEvents: 0
  - outbox pending: 106 / published: 100 / failed: 0 / retry: 0 / skipped: 0
- `GET /api/outbox/summary`
  - pending: 106, published: 100, failed: 0, retry: 0, skipped: 0
- `GET /api/routes/summary`
  - 총합: routePlans 206, delayed 57, deviated 19, approvalRequired 118, coldChainRisk 5, totalCost 58,075,330
- `GET /api/routes/summary?factoryId=FAC-A`
  - routePlans 74, delayed 20, deviated 7, approvalRequired 45, coldChainRisk 2
- `GET /api/routes/summary?date=2026-01-15`
  - 조건 충족 데이터 없음(0건) 또는 초기 적재 없음
- `GET /api/routes/summary?factoryId=FAC-A&date=2026-01-15`
  - 0건
- `POST /api/simulations/shipments?count=100`
  - 성공(요청 100건 이상 처리), 중복/실패 없음

## 4. `/api/routes/summary` 500 이슈 해결 여부
- 기존 증상: `JDBC parameter could not determine data type` 발생
- 분석:
  - `factoryId` 또는 `date`가 `null`일 때 JPQL 파라미터 타입 추론이 실패
- 조치:
  - 필터 조건을 **동적 조건 + 타입 명시/조건 분기** 방식으로 전환
  - summary 조회 프로젝션/쿼리를 안정화
- 최종:
  - 기본, `factoryId`, `date`, `factoryId+date` 모두 `HTTP 200`

## 5. Outbox / Publish 정상성
- `/api/outbox/publish`는 현재 Ledger disabled 상태에서 외부 호출 없이 DRY_RUN 또는 SKIPPED 경로로 동작
- outbox 이벤트는 PENDING/RETRY에서 대상 조회 후 상태 전환(published/failed/retry/skipped) 가능
- `retry_count` 및 `last_error` 기반 재시도 정책은 기존 배포 사양과 일치

## 6. 제출용 문서 반영
- `README.md`, `docs/final-smoke-result.md`, `docs/event-contract.md`, `docs/demo-scenario.md` 등과 연계
- 본문에서 핵심은 다음으로 정리됨
  - Archive-Logistics 역할
  - Synthetic Data 원칙
  - 이벤트 계약
  - Outbox/Batch 발행 구조
  - OCI-lite 설정

## 7. 리스크 및 잔여 작업
- 운영/운영 데모 시점에 따라 `date=2026-01-15` 결과가 0건일 수 있음(현재 DB 적재 데이터와 무관)
- Ledger를 실제 enabled로 전환한 후 실제 publish 연동 smoke는 별도 검증 필요

## 8. 최종 제출 한 줄
Archive-Logistics는 Archive-Nexus의 제조·출하 이벤트를 받아 synthetic route/ETA/비용을 계산하고 Ledger 비용 확정 이벤트를 발행하는 Spring Boot 물류 백엔드로, Outbox 격리, 배치 퍼블리싱, 장애 내성, 운영 요약/모니터링 API를 포함해 배포 가능한 품질로 정리했습니다.


