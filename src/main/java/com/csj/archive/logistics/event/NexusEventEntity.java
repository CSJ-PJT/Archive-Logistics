package com.csj.archive.logistics.event;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "nexus_logistics_event")
public class NexusEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    private String idempotencyKey;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NexusEventStatus status;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected NexusEventEntity() {
    }

    public NexusEventEntity(
            String eventId,
            String idempotencyKey,
            String source,
            String eventType,
            JsonNode payload,
            LocalDateTime now
    ) {
        this.eventId = eventId;
        this.idempotencyKey = idempotencyKey;
        this.source = source;
        this.eventType = eventType;
        this.schemaVersion = 1;
        this.payload = payload;
        this.status = NexusEventStatus.RECEIVED;
        this.receivedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markProcessed(LocalDateTime now) {
        this.status = NexusEventStatus.PROCESSED;
        this.processedAt = now;
        this.updatedAt = now;
    }

    public void markFailed(String failureReason, LocalDateTime now) {
        this.status = NexusEventStatus.FAILED;
        this.failureReason = failureReason;
        this.processedAt = now;
        this.updatedAt = now;
    }

    public Long id() {
        return id;
    }

    public String eventId() {
        return eventId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String eventType() {
        return eventType;
    }

    public String source() {
        return source;
    }

    public JsonNode payload() {
        return payload;
    }

    public NexusEventStatus status() {
        return status;
    }

    public LocalDateTime receivedAt() {
        return receivedAt;
    }

    public LocalDateTime processedAt() {
        return processedAt;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public String failureReason() {
        return failureReason;
    }
}
