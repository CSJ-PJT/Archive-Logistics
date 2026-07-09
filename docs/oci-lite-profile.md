# OCI Lite Profile

`oci-lite` is a deployment profile for low-memory environments such as OCI Free Tier instances.

It is not required for local development, but it documents the knobs that can be reduced when running on a small VM.

## JVM

Example:

```powershell
$env:JAVA_OPTS="-Xms128m -Xmx384m"
```

Container example:

```text
JAVA_OPTS=-Xms128m -Xmx384m
```

## HikariCP

Reduce connection pool size:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 3
      minimum-idle: 1
```

## Batch

Reduce chunk size:

```yaml
archive:
  outbox:
    chunk-size: 10
```

## Scheduler

Keep scheduler off by default or slow it down:

```yaml
archive:
  outbox:
    scheduler:
      enabled: false
      fixed-delay-ms: 60000
```

## Retention

For long-running low-memory environments, introduce short retention windows for:

- old audit logs
- old publish attempts
- published outbox events
- synthetic demo data

Retention cleanup should be explicit and should not remove pending/retry/failed events without operator review.

## Query Safety

Use pagination for operational queries:

```powershell
curl.exe "http://localhost:8092/api/routes/plans?page=0&size=50"
curl.exe "http://localhost:8092/api/outbox/events?page=0&size=50"
```

