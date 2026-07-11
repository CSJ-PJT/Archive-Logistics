package com.csj.archive.logistics.runtime;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class RuntimeEventControllerTest {
    private final RuntimeEventService runtimeEventService = org.mockito.Mockito.mock(RuntimeEventService.class);
    private final RuntimeEventController controller = new RuntimeEventController(runtimeEventService);

    @Test
    void recentEventsApiReturnsOk() throws Exception {
        when(runtimeEventService.recent(anyInt())).thenReturn(List.of(event()));

        var response = controller.recent(10);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().eventId()).isEqualTo("evt-runtime-1");
        assertThat(response.data().getFirst().sourceService()).isEqualTo("Archive-Logistics");
    }

    @Test
    void recentEventsAfterCursorApiReturnsOk() {
        when(runtimeEventService.recentAfter(eq("cursor-1"), anyInt())).thenReturn(List.of(event()));

        var response = controller.recent("cursor-1", 10);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().status()).isEqualTo("COMPLETED");
        assertThat(response.data().getFirst().idempotencyKey()).isEqualTo("RUNTIME:evt-runtime-1");
    }

    @Test
    void correlationEventsApiReturnsOk() throws Exception {
        when(runtimeEventService.byCorrelation(eq("CORR-1"))).thenReturn(List.of(event()));

        var response = controller.correlation("CORR-1");

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().correlationId()).isEqualTo("CORR-1");
    }

    @Test
    void entityEventsApiReturnsOk() throws Exception {
        when(runtimeEventService.byEntity(eq("ROUTE-1"))).thenReturn(List.of(event()));

        var response = controller.entity("ROUTE-1");

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().entityId()).isEqualTo("ROUTE-1");
    }

    private RuntimeEventResponse event() {
        return new RuntimeEventResponse(
                "evt-runtime-1",
                "Archive-Logistics",
                "logistics",
                "ROUTE_PLAN_CREATED",
                "route_plan",
                "ROUTE-1",
                "CORR-1",
                "CAUSE-1",
                "completed",
                "info",
                "Synthetic route calculated",
                LocalDateTime.parse("2026-07-10T10:00:00"),
                Map.of("routePlanId", "ROUTE-1")
        );
    }
}
