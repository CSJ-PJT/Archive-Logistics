package com.csj.archive.logistics.outbox;

import com.csj.archive.logistics.ledger.LedgerCompatibleEventPayload;
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
import java.time.ZoneOffset;

@Entity
@Table(name = "logistics_outbox_event")
public class LogisticsOutboxEntity {
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

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LogisticsOutboxEntity() {
    }

    public LogisticsOutboxEntity(LogisticsOutboxEvent event, LocalDateTime now) {
        this.eventId = event.eventId();
        this.idempotencyKey = event.idempotencyKey();
        this.source = event.source();
        this.eventType = event.eventType();
        this.aggregateType = event.aggregateType();
        this.aggregateId = event.aggregateId();
        this.payload = event.payload();
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public LedgerCompatibleEventPayload toLedgerPayload() {
        return new LedgerCompatibleEventPayload(
                eventId,
                idempotencyKey,
                source,
                eventType,
                aggregateType,
                aggregateId,
                1,
                createdAt.toInstant(ZoneOffset.UTC),
                payload
        );
    }

    public void markPublished(LocalDateTime now) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
        this.lastError = null;
        this.nextRetryAt = null;
        this.updatedAt = now;
    }

    public void scheduleRetry(String errorMessage, int maxRetryCount, LocalDateTime now) {
        this.retryCount += 1;
        this.lastError = trim(errorMessage);
        this.updatedAt = now;
        if (retryCount >= maxRetryCount) {
            this.status = OutboxStatus.FAILED;
            this.nextRetryAt = null;
            return;
        }
        this.status = OutboxStatus.RETRY;
        long delayMinutes = Math.min(60, 1L << Math.min(retryCount, 5));
        this.nextRetryAt = now.plusMinutes(delayMinutes);
    }

    public void resetForRetry(LocalDateTime now) {
        this.status = OutboxStatus.RETRY;
        this.nextRetryAt = now;
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

    public String source() {
        return source;
    }

    public String eventType() {
        return eventType;
    }

    public String aggregateType() {
        return aggregateType;
    }

    public String aggregateId() {
        return aggregateId;
    }

    public JsonNode payload() {
        return payload;
    }

    public OutboxStatus status() {
        return status;
    }

    public int retryCount() {
        return retryCount;
    }

    public String lastError() {
        return lastError;
    }

    public LocalDateTime nextRetryAt() {
        return nextRetryAt;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime publishedAt() {
        return publishedAt;
    }

    private String trim(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
