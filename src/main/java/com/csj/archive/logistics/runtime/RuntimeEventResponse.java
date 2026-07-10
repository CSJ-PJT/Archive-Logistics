package com.csj.archive.logistics.runtime;

import java.time.LocalDateTime;
import java.util.Map;

public record RuntimeEventResponse(
        String eventId,
        String sourceService,
        String domain,
        String eventType,
        String entityType,
        String entityId,
        String correlationId,
        String causationId,
        String status,
        String severity,
        String displayLabel,
        LocalDateTime occurredAt,
        Map<String, Object> metadata
) {
}
