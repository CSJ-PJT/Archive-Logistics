# Archive-Logistics

<p align="center">
  <img src="./docs/assets/logo.png" alt="ArchiveOS Logo" width="240" style="max-width: 280px; width: 40%; height: auto;" />
</p>

**출하 이벤트를 경로, ETA, 운송비로 변환해 Ledger로 발행하는 Spring Boot 물류 서비스**

Archive-Logistics는 Archive Platform Ecosystem에서 물류 이벤트 변환 계층을 담당합니다. Archive-Nexus가 발행한 제조/출하 이벤트를 수신하고, deterministic synthetic route calculator로 route plan, ETA, 운송비, 지연/우회 비용을 계산한 뒤 Archive-Ledger가 처리할 수 있는 비용 확정 이벤트를 outbox에 저장하고 발행합니다.

외부 표시명은 `Archive-Logistics`로 통일합니다. 일부 내부 source, class, artifact, outbox source에는 기존 계약 호환성을 위해 `Archive-Logitics` 또는 `logitics` 표기가 남아 있을 수 있습니다.

## Operational Role

- Nexus 물류 이벤트 수신
- synthetic route / ETA / cost 계산
- route_plan, route_cost 저장
- PostgreSQL outbox 저장
- Spring Batch Publisher 기반 Ledger publish
- Ledger 장애/비활성 상태에서 DRY_RUN/SKIPPED 처리
- route/cost/outbox/audit 운영 조회 제공

## Stack

- Java 21
- Spring Boot 3
- Spring Web / Validation / Data JPA
- PostgreSQL
- Flyway
- Spring Batch
- Spring Actuator / Micrometer
- Docker / Docker Compose
- JUnit 5 / AssertJ / Testcontainers

## Main APIs

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/operations/summary` | service, outbox, risk, ledger, memory 운영 요약 |
| `GET` | `/api/routes/summary` | route/cost 집계 조회 |
| `POST` | `/api/events/nexus/bulk` | Nexus bulk 물류 이벤트 수신 |
| `POST` | `/api/events/nexus` | Nexus 단건 이벤트 수신 |
| `POST` | `/api/simulations/shipments` | synthetic shipment 데모 데이터 생성 |
| `GET` | `/api/outbox/summary` | outbox 상태 집계 |
| `POST` | `/api/outbox/publish` | Ledger publish 또는 dry-run 처리 |
| `GET` | `/actuator/health` | actuator health |

## Operations Dashboard

Archive-Logistics includes a lightweight operational dashboard served by Spring Boot static resources.

- Dashboard: `http://localhost:8092/`
- Alias: `http://localhost:8092/dashboard.html`

The dashboard shows the end-to-end process:

```text
Archive-Nexus -> Route Calculator -> Outbox -> Archive-Ledger -> ArchiveOS
```

It reads existing APIs only:

- `/api/operations/summary`
- `/api/routes/summary`
- `/api/outbox/summary`
- `/api/simulations/shipments`
- `/api/outbox/publish`

## Failure Isolation

Ledger 연동은 DB Outbox Pattern으로 격리합니다. Nexus 이벤트 수신과 route/cost 계산은 하나의 트랜잭션에서 처리하고, 외부 Ledger 발행은 outbox publisher가 별도로 수행합니다.

- `archive.ledger.enabled=false`: 외부 호출 없이 DRY_RUN/SKIPPED 처리
- Ledger 장애: Archive-Logistics API는 생존, outbox에 실패 상태 저장
- 재시도 추적: `retry_count`, `last_error`, `next_retry_at`
- 최대 재시도 이후 `FAILED` 상태 전환
- publish attempt는 `ledger_publish_attempt`에 기록

## `/api/routes/summary` Fix

초기 구현에서는 nullable `date`, `factoryId`를 JPQL 조건에 직접 바인딩하면서 PostgreSQL JDBC가 파라미터 타입을 추론하지 못해 `could not determine data type` 500 오류가 발생할 수 있었습니다.

현재는 조건 조합별 Repository/Service 분기 로직으로 전환했습니다.

- `GET /api/routes/summary` -> HTTP 200
- `GET /api/routes/summary?factoryId=FAC-A` -> HTTP 200
- `GET /api/routes/summary?date=2026-01-15` -> HTTP 200
- `GET /api/routes/summary?factoryId=FAC-A&date=2026-01-15` -> HTTP 200

자세한 내용은 [routes-summary-fix.md](docs/routes-summary-fix.md)를 참고합니다.

## Local Run

```powershell
docker compose up --build
```

또는 로컬 PostgreSQL이 준비된 상태에서:

```powershell
.\gradlew.bat bootRun
```

기본 포트:

- App: `http://localhost:8092`
- PostgreSQL: `localhost:5434`
- Expected Ledger: `http://localhost:8093` 또는 profile 설정값

## Smoke Test

```powershell
curl.exe http://localhost:8092/actuator/health
curl.exe http://localhost:8092/
curl.exe http://localhost:8092/api/operations/summary
curl.exe -X POST "http://localhost:8092/api/simulations/shipments?count=100"
curl.exe http://localhost:8092/api/routes/summary
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A"
curl.exe "http://localhost:8092/api/routes/summary?date=2026-01-15"
curl.exe "http://localhost:8092/api/routes/summary?factoryId=FAC-A&date=2026-01-15"
curl.exe http://localhost:8092/api/outbox/summary
curl.exe -X POST http://localhost:8092/api/outbox/publish
```

Ledger disabled 상태에서 publish는 외부 호출 없이 DRY_RUN/SKIPPED로 종료됩니다.

## Runbook

운영 점검 순서:

1. `GET /actuator/health`로 JVM/Spring 상태를 확인합니다.
2. `GET /api/operations/summary`에서 `failedEvents`, `outbox.failed`, `outbox.retry`, `ledger.status`를 확인합니다.
3. `GET /api/routes/summary`로 route/cost 집계가 정상인지 확인합니다.
4. `GET /api/outbox/summary`로 pending/retry/failed 비율을 확인합니다.
5. Ledger disabled 환경이면 publish 결과가 DRY_RUN/SKIPPED인지 확인합니다.
6. Ledger enabled 환경에서 실패가 증가하면 `last_error`, `retry_count`, `ledger_publish_attempt`를 확인합니다.

상세 운영 절차는 [operations-runbook.md](docs/operations-runbook.md)를 참고합니다.

## Documentation

- [Architecture](docs/architecture.md)
- [Event Contract](docs/event-contract.md)
- [Route Summary Fix](docs/routes-summary-fix.md)
- [Outbox Batch Publisher](docs/outbox-batch-publisher.md)
- [API Reference](docs/api-reference.md)
- [Smoke Test](docs/smoke-test.md)
- [Operations Runbook](docs/operations-runbook.md)
- [OCI Lite Profile](docs/oci-lite-profile.md)
- [API Examples](docs/api-examples.http)

## Synthetic Data Policy

Archive-Logistics는 실제 지도 API, 실제 배송 데이터, 실제 차량 데이터, 실제 주소, 개인정보를 사용하지 않습니다. 모든 route, ETA, cost, risk는 synthetic matrix와 deterministic hash로 계산됩니다.
