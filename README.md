# Archive-Logitics

<p align="center">
  <img src="./docs/assets/logo.png" alt="ArchiveOS logo" width="220" />
</p>

Archive-Logitics는 Archive-Nexus의 제조·출하 이벤트를 수신해 synthetic route, ETA, 운송비, 지연/우회 비용을 계산하고 Archive-Ledger로 물류비 확정 이벤트를 발행하는 Spring Boot 기반 물류 이벤트 서비스입니다.  
저장소명은 `Archive-Logitics`를 유지하고 Java 패키지/도메인은 `logistics`를 사용합니다.

## Archive Ecosystem 역할

- Archive-Nexus: 제조/출하 원인 이벤트를 발생
- Archive-Logitics: 물류 경로/비용 산정, 검증 가능한 정형 아웃박스 생성, Ledger 이벤트 전달
- Archive-Ledger: 비용 확정 이벤트를 정산/원장/거래로 반영
- ArchiveOS: 고액/위험 이벤트의 승인·감사·알림(Archive-Ledger 연동 데이터) 처리

실시간 지도/차량/사용자 데이터는 사용하지 않으며, synthetic location/거리/가격 규칙으로만 계산합니다.

## Stack

Java 21, Spring Boot 3, Spring Web, Validation, Spring Data JPA, PostgreSQL, Flyway, Spring Batch, Actuator, Micrometer, springdoc-openapi, JUnit 5, AssertJ, Testcontainers, Docker Compose, GitHub Actions.

Lombok, Kafka, Redis, OSRM/GraphHopper, 실제 지도 API, 실제 배송/차량/주소/PII는 사용하지 않습니다.

## Local Run

```powershell
docker compose up --build
.\gradlew.bat clean test
.\gradlew.bat build
.\gradlew.bat bootRun
```

- App: `http://localhost:8092`
- PostgreSQL: `localhost:5434`(기본 예시)
- Actuator: `http://localhost:8092/actuator/health`

## 주요 API

- `POST /api/events/nexus`
- `POST /api/events/nexus/bulk`
- `POST /api/simulations/shipments?count=<N>`
- `GET /api/routes/plans`
- `GET /api/routes/plans/{routePlanId}`
- `GET /api/routes/costs`
- `GET /api/routes/costs/{routePlanId}`
- `GET /api/routes/summary`
- `GET /api/routes/summary?factoryId=FAC-A`
- `GET /api/routes/summary?date=YYYY-MM-DD`
- `GET /api/routes/summary?factoryId=FAC-A&date=YYYY-MM-DD`
- `GET /api/outbox/summary`
- `POST /api/outbox/publish`
- `GET /api/operations/summary`
- `GET /actuator/health`

## /api/routes/summary 500 이슈

초기 구현에서 `date`, `factoryId`를 nullable 바인딩하는 JPQL 조건식으로 인해 PostgreSQL에서 `could not determine data type` 500 에러가 발생할 수 있었습니다.  
현재는 nullable 조건 분기 쿼리를 제거하고 날짜/팩토리 조합별 조회 분기 경로로 전환하여 해결했습니다.

## Ledger 연동

기본값:

```yaml
archive:
  ledger:
    enabled: false
    base-url: http://localhost:18080
    bulk-endpoint: /api/events/nexus/bulk
    contract-mode: ARCHIVE_LEDGER_V1_COMPAT
```

`enabled=false` 상태에서는 `/api/outbox/publish` 응답이 `DRY_RUN`이며 외부 전송을 하지 않습니다.

## Quick Demo

```powershell
curl.exe http://localhost:8092/actuator/health
curl.exe http://localhost:8092/api/operations/summary
curl.exe -X POST "http://localhost:8092/api/simulations/shipments?count=100"
curl.exe http://localhost:8092/api/outbox/summary
curl.exe -X POST http://localhost:8092/api/outbox/publish
curl.exe "http://localhost:8092/api/routes/plans?page=0&size=20"
```

## OCI-lite

`application-oci-lite.yml`는 저메모리 배포를 위한 설정 템플릿입니다.

- JVM: `-Xms128m -Xmx384m`
- Hikari 풀/Batch 청크/스케줄러/재시도 제한 축소

## Portfolio Line

Archive-Logistics는 Archive-Nexus의 제조·출하 이벤트를 수신해 synthetic route, ETA, 운송비, 지연/우회 비용을 계산하고 Archive-Ledger로 물류비 확정 이벤트를 발행하는 Spring Boot 기반 물류 이벤트 서비스입니다. PostgreSQL + Flyway 기반 Outbox Pattern, idempotency key 중복 방지, Spring Batch Publisher, Ledger 장애 격리, Actuator 운영 요약 API를 통해 제조 → 물류 → 정산 흐름을 안정적으로 연결했습니다.
