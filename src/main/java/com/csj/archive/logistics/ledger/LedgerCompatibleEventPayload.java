package com.csj.archive.logistics.ledger;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record LedgerCompatibleEventPayload(
        String eventId,
        String idempotencyKey,
        String source,
        String eventType,
        String aggregateType,
        String aggregateId,
        Integer schemaVersion,
        Instant occurredAt,
        JsonNode payload
) {
}
