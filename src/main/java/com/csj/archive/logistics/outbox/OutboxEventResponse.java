package com.csj.archive.logistics.outbox;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record OutboxEventResponse(
        String eventId,
        String idempotencyKey,
        String source,
        String eventType,
        String aggregateType,
        String aggregateId,
        JsonNode payload,
        String status,
        int retryCount,
        String lastError,
        LocalDateTime nextRetryAt,
        LocalDateTime createdAt,
        LocalDateTime publishedAt
) {
    static OutboxEventResponse from(LogisticsOutboxEntity entity) {
        return new OutboxEventResponse(
                entity.eventId(),
                entity.idempotencyKey(),
                entity.source(),
                entity.eventType(),
                entity.aggregateType(),
                entity.aggregateId(),
                entity.payload(),
                entity.status().name(),
                entity.retryCount(),
                entity.lastError(),
                entity.nextRetryAt(),
                entity.createdAt(),
                entity.publishedAt()
        );
    }
}
