package com.csj.archive.logistics.outbox;

import com.fasterxml.jackson.databind.JsonNode;

public record LogisticsOutboxEvent(
        String eventId,
        String idempotencyKey,
        String source,
        String eventType,
        String aggregateType,
        String aggregateId,
        JsonNode payload
) {
}
