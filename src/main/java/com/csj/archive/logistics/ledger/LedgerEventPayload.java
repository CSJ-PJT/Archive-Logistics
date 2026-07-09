package com.csj.archive.logistics.ledger;

import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record LedgerEventPayload(
        String eventId,
        String idempotencyKey,
        String source,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        JsonNode payload
) {
    public static LedgerEventPayload fromOutbox(LogisticsOutboxEntity entity) {
        return new LedgerEventPayload(
                entity.eventId(),
                entity.idempotencyKey(),
                entity.source(),
                entity.eventType(),
                1,
                Instant.now(),
                entity.payload()
        );
    }
}
