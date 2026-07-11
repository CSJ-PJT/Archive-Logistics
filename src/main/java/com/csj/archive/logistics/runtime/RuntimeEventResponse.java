package com.csj.archive.logistics.runtime;

import java.time.LocalDateTime;
import java.util.Map;

public record RuntimeEventResponse(
        String eventId,
        String idempotencyKey,
        String sourceService,
        String targetService,
        String domain,
        String eventType,
        String entityType,
        String entityId,
        String correlationId,
        String causationId,
        String simulationRunId,
        String settlementCycleId,
        String workdayId,
        String status,
        String severity,
        String displayLabel,
        LocalDateTime occurredAt,
        int hopCount,
        int maxHop,
        Map<String, Object> metadata,
        String cursor
) {
    /**
     * Compatibility constructor for the original Live Flow projection contract.
     * New Runtime Mesh fields are derived from synthetic metadata where available.
     */
    public RuntimeEventResponse(
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
        this(
                eventId,
                value(metadata, "idempotencyKey", "RUNTIME:" + eventId),
                sourceService,
                value(metadata, "targetService", "ArchiveOS"),
                domain,
                eventType,
                entityType,
                entityId,
                correlationId,
                causationId,
                value(metadata, "simulationRunId", null),
                value(metadata, "settlementCycleId", null),
                value(metadata, "workdayId", null),
                normalize(status),
                normalize(severity),
                displayLabel,
                occurredAt,
                intValue(metadata, "hopCount", 0),
                intValue(metadata, "maxHop", 5),
                metadata == null ? Map.of() : Map.copyOf(metadata),
                RuntimeEventCursor.encode(occurredAt, eventId)
        );
    }

    private static String value(Map<String, Object> metadata, String key, String fallback) {
        if (metadata == null || metadata.get(key) == null) {
            return fallback;
        }
        String value = String.valueOf(metadata.get(key));
        return value.isBlank() ? fallback : value;
    }

    private static int intValue(Map<String, Object> metadata, String key, int fallback) {
        if (metadata == null || metadata.get(key) == null) {
            return fallback;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "UNKNOWN" : value.toUpperCase(java.util.Locale.ROOT);
    }
}
