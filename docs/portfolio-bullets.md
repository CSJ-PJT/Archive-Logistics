Archive-Logitics · Java/Spring Synthetic Logistics Event Backend

Archive-Logitics는 Archive-Nexus의 제조·출하 이벤트를 수신해 synthetic route, ETA, 운송비, 지연/우회 비용을 계산하고 Archive-Ledger로 물류비 확정 이벤트를 발행하는 Java/Spring Boot 기반 물류 이벤트 서비스를 구현했습니다. PostgreSQL + Flyway 기반 Outbox Pattern, idempotency key 중복 방지, Spring Batch Publisher, Ledger 장애 격리, Actuator 운영 요약 API를 통해 제조 → 물류 → 정산 흐름을 안정적으로 연결했습니다.

- Spring Boot 기반 Logistics 이벤트 처리 파이프라인(입력 검증, 단건/배치 수신, route_plan/route_cost 생성)
- PostgreSQL + JPA + Flyway 정합성 마이그레이션 구성
- DB Outbox 패턴 + Spring Batch Publisher로 Ledger 전송 분리
- DRY\_RUN/동작 모드 분기와 장애 격리 구조
- Audit log/운영 요약 API를 포함한 운영 가시성
- OCI-lite를 고려한 프로필 기반 안정성 가이드 정비
