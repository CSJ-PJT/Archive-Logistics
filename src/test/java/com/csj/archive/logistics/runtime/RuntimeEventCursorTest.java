package com.csj.archive.logistics.runtime;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeEventCursorTest {
    @Test
    void cursorRoundTripPreservesTimestampAndEventId() {
        RuntimeEventResponse event = new RuntimeEventResponse(
                "evt-runtime-1", "Archive-Logistics", "logistics", "ROUTE_ASSIGNED",
                "route_plan", "ROUTE-1", "CORR-1", "CAUSE-1", "completed", "info",
                "Synthetic route assigned", LocalDateTime.parse("2026-07-11T10:00:00"), Map.of()
        );

        String cursor = RuntimeEventCursor.encode(event);

        assertThat(event.cursor()).isEqualTo(cursor);
        assertThat(RuntimeEventCursor.decode(cursor)).hasValueSatisfying(position -> {
            assertThat(position.occurredAt()).isEqualTo(event.occurredAt());
            assertThat(position.eventId()).isEqualTo(event.eventId());
        });
    }

    @Test
    void invalidCursorIsRejectedWithoutThrowing() {
        assertThat(RuntimeEventCursor.decode("not-a-runtime-cursor")).isEmpty();
    }
}
