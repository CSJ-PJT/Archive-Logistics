package com.csj.archive.logistics.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "archiveos_runtime_delivery")
public class ArchiveOsRuntimeDeliveryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "event_id", unique = true, nullable = false) private String eventId;
    @Column(name = "idempotency_key", nullable = false) private String idempotencyKey;
    @Column(name = "correlation_id") private String correlationId;
    @Column(name = "causation_id") private String causationId;
    @Column(name = "order_id") private String orderId;
    @Column(name = "simulation_run_id") private String simulationRunId;
    @Column(name = "entity_type", nullable = false) private String entityType;
    @Column(name = "entity_id", nullable = false) private String entityId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "source_system", nullable = false) private String sourceSystem;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "payload_json", columnDefinition = "jsonb", nullable = false) private JsonNode payload;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ArchiveOsDeliveryStatus status;
    @Column(name = "retry_count", nullable = false) private int retryCount;
    @Column(name = "next_retry_at") private LocalDateTime nextRetryAt;
    @Column(name = "last_error_code") private String lastErrorCode;
    @Column(name = "last_error_message") private String lastErrorMessage;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "publishing_started_at") private LocalDateTime publishingStartedAt;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected ArchiveOsRuntimeDeliveryEntity() { }

    public ArchiveOsRuntimeDeliveryEntity(String eventId, String key, String correlation, String causation, String order,
                                          String simulation, String entityType, String entityId, String eventType,
                                          JsonNode payload, LocalDateTime now) {
        this.eventId = eventId; this.idempotencyKey = key; this.correlationId = correlation; this.causationId = causation;
        this.orderId = order; this.simulationRunId = simulation; this.entityType = entityType; this.entityId = entityId;
        this.eventType = eventType; this.sourceSystem = "archive-logistics"; this.payload = payload;
        this.status = ArchiveOsDeliveryStatus.PENDING; this.createdAt = now; this.updatedAt = now;
    }

    public String eventId() { return eventId; }
    public String correlationId() { return correlationId; }
    public String causationId() { return causationId; }
    public String orderId() { return orderId; }
    public String eventType() { return eventType; }
    public ArchiveOsDeliveryStatus status() { return status; }
    public int retryCount() { return retryCount; }
    public JsonNode payload() { return payload; }
    public LocalDateTime publishedAt() { return publishedAt; }
    public LocalDateTime publishingStartedAt() { return publishingStartedAt; }
    public LocalDateTime nextRetryAt() { return nextRetryAt; }
    public String lastErrorCode() { return lastErrorCode; }
    public LocalDateTime updatedAt() { return updatedAt; }

    public void publishing(LocalDateTime now) { status = ArchiveOsDeliveryStatus.PUBLISHING; publishingStartedAt = now; updatedAt = now; }
    public void published(LocalDateTime now) { status = ArchiveOsDeliveryStatus.PUBLISHED; publishedAt = now; publishingStartedAt = null; updatedAt = now; nextRetryAt = null; lastErrorCode = null; lastErrorMessage = null; }
    public void fail(ArchiveOsDeliveryStatus nextStatus, String code, String message, LocalDateTime next) {
        status = nextStatus; lastErrorCode = code; lastErrorMessage = message; nextRetryAt = next; retryCount++; publishingStartedAt = null; updatedAt = LocalDateTime.now();
    }
    public void recoverStale(LocalDateTime now, int maxRetryCount) {
        publishingStartedAt = null;
        if (retryCount >= maxRetryCount) { status = ArchiveOsDeliveryStatus.FAILED; nextRetryAt = null; }
        else { status = ArchiveOsDeliveryStatus.RETRY_WAIT; retryCount++; nextRetryAt = now.plusSeconds(30); lastErrorCode = "STALE_PUBLISHING_RECOVERED"; lastErrorMessage = "Publishing lease expired"; }
        updatedAt = now;
    }
}
