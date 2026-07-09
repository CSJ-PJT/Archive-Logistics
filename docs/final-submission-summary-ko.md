# Archive-Logistics 최종 제출 요약 (한국어)

## 1. 시스템 개요
- **Archive-Logistics**는 Archive-Nexus가 발행하는 출하/물류 이벤트를 수신해  
  synthetic route(합성 경로), ETA, 운송비, 지연·우회(Deviation) 비용을 계산하고  
  Ledger 정산용 비용 확정 이벤트를 안정적으로 발행하는 Spring Boot 백엔드입니다.
- 본 서비스는 실제 지도/물류 데이터 없이, 고정된 **Synthetic Matrix**와 Deterministic 로직으로 비용·위험을 산정합니다.

## 2. 최종 검증 결과
- `/api/routes/summary` 500 이슈 해결 완료  
  - 원인: JPQL에서 `null` 파라미터 바인딩 시 PostgreSQL 데이터 타입 미결정 오류
  - 조치: 조건 필터를 `CriteriaBuilder`/명시 타입 바인딩 방식으로 변경
  - 확인: `HTTP 200` 정상 응답 (기본/`factoryId`/`date`/`factoryId+date`)
- `Ledger` 연동은 현재 **disabled** 상태
  - `/api/outbox/publish`는 `DRY_RUN / SKIPPED` 형태로 외부 호출 없이 정상 처리
- Smoke 검증 API 응답
  - `GET /actuator/health` : UP
  - `GET /api/operations/summary` : 서비스 Healthy, Outbox pending 106, published 100
  - `GET /api/routes/summary` : routePlans 206

## 3. 핵심 아웃박스/복원성
- PostgreSQL 기반 Outbox 테이블 + Spring Batch 발행 Job으로 Ledger 연계격리 구현
- Ledger 장애가 발생해도 Archive-Logistics 자체는 유지되며, 장애는 outbox 상태/재시도로 격리

## 4. 최종 산출
- Archive-Nexus 이벤트 수신, idempotency 중복 방지, route/cost 생성, outbox 기록이 모두 동작
- route summary 필터링(공장/날짜) 및 조회 API 정상 동작
- 운영 문서와 제출 문서 정리 완료

## 5. 최종 문장
Archive-Logistics는 **Archive-Nexus 이벤트 → Synthetic 물류 계산 → Ledger 비용 이벤트 발행**을 안정적으로 잇는 물류 이벤트 백엔드로, 중복 방지·재시도·운영 모니터링 요건을 갖춘 제출 가능한 형태로 정리 완료했습니다.


