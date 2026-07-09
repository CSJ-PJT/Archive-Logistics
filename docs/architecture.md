# Architecture

Archive-Logistics는 Archive Platform Ecosystem의 물류 이벤트 변환 계층입니다.

```text
Archive-Nexus -> Archive-Logistics -> Archive-Ledger -> ArchiveOS
```

## Responsibilities

- Nexus 이벤트 수신과 idempotency key 중복 방지
- deterministic synthetic distance matrix 기반 route/ETA 계산
- fuel/toll/urgent/delay/cold-chain cost 계산
- Ledger 비용 확정 이벤트 생성
- DB Outbox 저장과 Batch Publisher
- Ledger 장애 격리와 retry 상태 관리
- audit_log와 operations summary 제공

## Failure Isolation

Ledger가 꺼져 있거나 장애가 있어도 Nexus 이벤트 수신과 route/cost 계산은 계속 동작합니다. 외부 송신은 outbox publisher가 별도로 수행하며 실패 시 `RETRY` 또는 `FAILED`로 상태를 남깁니다.

## Synthetic Routing

실제 지도 API, OSRM, GraphHopper, 실제 주소/차량/운송사 데이터는 사용하지 않습니다. 모든 거리와 리스크는 코드 내부 matrix와 deterministic hash로 계산됩니다.

