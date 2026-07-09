# Portfolio Bullets

Archive-Logitics · Java/Spring Synthetic Logistics Event Backend

Archive-Nexus의 제조/출하 이벤트를 수신해 synthetic route, ETA, 물류비, 지연/우회 이벤트를 계산하고 Archive-Ledger로 비용 확정 이벤트를 발행하는 Java/Spring Boot 기반 물류 이벤트 서비스를 구현했습니다. PostgreSQL + Flyway 기반 Outbox 패턴, idempotency key 중복 방지, Spring Batch Publisher, Ledger 장애 격리, Actuator 운영 요약 API를 구성해 제조 → 물류 → 정산 흐름을 안정적으로 연결했습니다.

- Java 21, Spring Boot 3, JPA, PostgreSQL, Flyway 기반 물류 이벤트 백엔드
- Deterministic synthetic route calculator로 실제 지도/배송 데이터 없이 재현 가능한 비용 계산
- DB Outbox Pattern과 Spring Batch Publisher로 Ledger 장애 격리
- idempotency key 기반 중복 수신 방지와 audit log 기록
- Testcontainers PostgreSQL 기반 통합 테스트와 Docker Compose 로컬 실행 구성
