# Low Memory And OCI Profile

Local PC usage can run full Java/Spring + PostgreSQL through Docker Compose. OCI free tier deployment should use `oci-lite`.

```bash
SPRING_PROFILES_ACTIVE=oci-lite
JAVA_OPTS="-Xms128m -Xmx384m"
```

Recommended low-memory controls:

- HikariCP: `DB_POOL_SIZE=3`, `DB_MIN_IDLE=1`
- Outbox chunk: `ARCHIVE_OUTBOX_CHUNK_SIZE=20`
- Scheduler: keep disabled unless needed
- Scheduler interval: `ARCHIVE_OUTBOX_SCHEDULER_FIXED_DELAY_MS=60000` or higher
- Tomcat: `TOMCAT_MAX_THREADS=40`
- Use pagination for all large route/outbox queries
- Add retention cleanup later for old `audit_log`, `ledger_publish_attempt`, and published outbox rows

The service is designed so Ledger publish can remain disabled while the core event processing path stays usable.
