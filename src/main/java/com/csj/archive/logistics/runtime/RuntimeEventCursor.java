package com.csj.archive.logistics.runtime;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

final class RuntimeEventCursor {
    private RuntimeEventCursor() {
    }

    static String encode(RuntimeEventResponse event) {
        if (event == null || event.occurredAt() == null || event.eventId() == null) {
            return null;
        }
        return encode(event.occurredAt(), event.eventId());
    }

    static String encode(LocalDateTime occurredAt, String eventId) {
        if (occurredAt == null || eventId == null || eventId.isBlank()) {
            return null;
        }
        String value = occurredAt + "|" + eventId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static Optional<Position> decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return Optional.empty();
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = decoded.indexOf('|');
            if (separator <= 0 || separator == decoded.length() - 1) {
                return Optional.empty();
            }
            return Optional.of(new Position(
                    LocalDateTime.parse(decoded.substring(0, separator)),
                    decoded.substring(separator + 1)
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    record Position(LocalDateTime occurredAt, String eventId) {
    }
}
