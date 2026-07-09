# Final Smoke Result (Archive-Logitics)

## 1) 작업 범위
- Archive-Logitics 역할 검증
- `/api/routes/summary` 500 이슈 해결 확인
- route/cost 생성 및 조회 smoke
- Ledger 연동 publish 동작 점검
- 테스트/빌드/compose 설정 확인

## 2) 핵심 점검 항목
- 실행 환경: `local` profile, DB PostgreSQL(컨테이너), `ARCHIVE_LEDGER_ENABLED=false`
- 요청일: `2026-07-09`

### 2.1 `/api/routes/summary` 해결 여부
- 이전 상태: null 바인딩 JPQL로 `JDBC parameter could not determine data type` 발생 (`500`)
- 조치: `RoutePlanRepository` null 조건 쿼리 제거, 조건별 분기 조회 API 경로로 전환
- 현재:  
  - `GET /api/routes/summary` → `200`
  - `GET /api/routes/summary?factoryId=FAC-A` → `200`
  - `GET /api/routes/summary?date=2026-01-15` → `200` (해당일 데이터 부재 시 0 반환)
  - `GET /api/routes/summary?factoryId=FAC-A&date=2026-01-15` → `200`

## 3) route/cost smoke 결과
- `POST /api/simulations/shipments?count=100` → `200`
  - `requestedCount=100`, `processedCount=100`, `outboxCreatedCount=100`
- `GET /api/routes/summary` → `routePlans=200`, `delayedRoutes=55`, `deviatedRoutes=19`, `approvalRequired=116`, `coldChainRisk=5`, `totalCost=56908070`
- `GET /api/routes/summary?factoryId=FAC-A` → `routePlans=72`
- `GET /api/routes/plans?page=0&size=5` → `200` (목록 조회)
- `GET /api/routes/costs?page=0&size=5` → `200` (목록 조회)
- `GET /api/routes/plans/{routePlanId}` → `200`
- `GET /api/routes/costs/{routePlanId}` → `200`
- 운영집계에서 `receivedEvents=200`, `processedEvents=200`, `failedEvents=0`, `routePlans=200`

## 4) outbox / publish 결과
- `GET /api/outbox/summary` → `pending=100`, `published=100`, `failed=0`, `retry=0`, `skipped=0`
- `POST /api/outbox/publish` → `200`, `dryRun=true`, `status=DRY_RUN`, `skippedCount=50`
- `ledger.enabled=false` 유지: 외부 Ledger 전송 없이 안전 처리

## 5) Ledger disabled / dry-run 여부
- Ledger 연동 상태: `false`
- 처리 결과: **DRY_RUN/SKIPPED 동작 정상**
- `archive.ledger.base-url`, `bulk-endpoint`, `contract-mode`는 `Archive-Logitics` 구성값으로 노출됨

## 6) ArchiveOS 읽는 API 목록
- ArchiveOS가 이 저장소 API를 직접 호출하는 항목: 현재는 없음(범위상 Ledger 전용 계약 이벤트 전달만 존재)
- 간접 경로(동일 생태계 흐름): Archive-Ledger가 Archive-Logitics가 발행한 비용 확정 이벤트를 수신/처리
- Archive-Logitics에서 ArchiveOS가 기대할 수 있는 참조 조회 API:
  - `GET /api/operations/summary`
  - `GET /api/routes/summary`

## 7) 남은 이슈
- 없음 (현재 검증 항목은 모두 통과)
