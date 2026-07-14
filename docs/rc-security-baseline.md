# RC Security Baseline

## Exposure audit

| Port/API | RC exposure | Required access | Control |
|---|---|---|---|
| `8092` application | `127.0.0.1` only | local gateway/reverse proxy | Compose loopback bind |
| PostgreSQL `5432` | Docker network only | Archive-Logistics | `expose`, no host port |
| `POST /api/events/nexus` | internal | Archive-Nexus | `logistics:ingest` token, source and scope headers |
| Admin write APIs | internal admin | ArchiveOS/operator | `admin:operate` token, source and scope headers |
| Sensitive reads | internal | ArchiveOS/operator | `runtime:read` token, source and scope headers |
| `/actuator/health`, `/actuator/info` | limited public | probes | explicitly public |

## RC configuration

`rc` requires `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `ARCHIVE_INTERNAL_SERVICE_TOKEN`, and `ARCHIVE_ADMIN_SERVICE_TOKEN`. No credential values are stored in this repository. Springdoc and non-health Actuator endpoints are disabled in this profile.

## Header contract

```
Authorization: Bearer <service-token>
X-Archive-Source-System: Archive-Nexus
X-Archive-Service-Scope: logistics:ingest
```

Missing or invalid tokens return `401`; a source or scope mismatch returns `403`. Clients must classify `401/403` as credential configuration failures and must not retry them through the outbox retry loop.

## Rotation

Generate a replacement token in the deployment secret store, inject it into the calling service and this service, validate one authenticated request, then recreate the containers. During a planned grace period accept only explicitly configured previous credentials; revoke the previous credential and inspect logs for accidental header/token output. Tokens are never written to application logs or committed files.

## Remaining production controls

This RC baseline does not add mTLS, an external secret manager, gateway rate limiting, or Kubernetes network policies. Those controls remain deployment-platform responsibilities.
