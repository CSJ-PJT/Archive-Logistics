package com.csj.archive.logistics.runtime;

import com.csj.archive.logistics.common.DeterministicHash;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.event.NexusLogisticsEventRequest;
import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.csj.archive.logistics.outbox.LogisticsOutboxEvent;
import com.csj.archive.logistics.route.RouteCost;
import com.csj.archive.logistics.route.RouteCostEntity;
import com.csj.archive.logistics.route.RoutePlan;
import com.csj.archive.logistics.route.RoutePlanEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ArchiveOsRouteOutboxProjectionServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC);
    private final ArchiveOsRuntimeDeliveryService delivery = mock(ArchiveOsRuntimeDeliveryService.class);
    private final ArchiveOsRouteOutboxProjectionService service = new ArchiveOsRouteOutboxProjectionService(delivery, new IdGenerator(clock, new DeterministicHash()));

    @Test
    void projectsRouteFactsOnceWithNullLineagePreservedAndNoPii() {
        RoutePlan plan = new RoutePlan("ROUTE-1", "source-event-1", "SHIP-1", "FAC-A", "FAC-A", "DC-1", "VENDOR-1",
                null, "customer-should-not-leak", "VIP", "battery", null, null, "HIGH", null, false, false,
                null, null, "SIM-1", null, 0, 5, BigDecimal.ONE, 40, "HIGH", new BigDecimal("0.1000"), false, false, false,
                "ROUTE_ASSIGNED", new RouteCost(1, 2, 3, 0, 0, 6, "KRW", false, "reason"));
        RoutePlanEntity route = new RoutePlanEntity(plan, LocalDateTime.now(clock));
        RouteCostEntity cost = new RouteCostEntity(plan, LocalDateTime.now(clock));
        LogisticsOutboxEntity outbox = new LogisticsOutboxEntity(new LogisticsOutboxEvent("ledger-event-1", "LOGISTICS:LOGISTICS_COST_CONFIRMED:ROUTE-1",
                "Archive-Logitics", "LOGISTICS_COST_CONFIRMED", "ROUTE_PLAN", "ROUTE-1", new ObjectMapper().createObjectNode()), LocalDateTime.now(clock));
        NexusLogisticsEventRequest request = new NexusLogisticsEventRequest("source-event-1", "NEXUS:1", "Archive-Nexus", "LOGISTICS_DISPATCHED", Instant.now(clock),
                new NexusLogisticsEventRequest.Payload("FAC-A", "SHIP-1", "FAC-A", "DC-1", "HIGH", "battery", 1, false));

        service.routeCreated(request, route, cost, outbox, LocalDateTime.now(clock));

        ArgumentCaptor<ArchiveOsRuntimeDeliveryService.Projection> projected = ArgumentCaptor.forClass(ArchiveOsRuntimeDeliveryService.Projection.class);
        verify(delivery, times(4)).schedule(projected.capture());
        List<ArchiveOsRuntimeDeliveryService.Projection> values = projected.getAllValues();
        assertThat(values).extracting(ArchiveOsRuntimeDeliveryService.Projection::eventType)
                .containsExactly("SHIPMENT_CREATED", "ROUTE_ASSIGNED", "ROUTE_COST_CALCULATED", "LOGISTICS_COST_CONFIRMED");
        assertThat(values).allSatisfy(value -> {
            assertThat(value.correlationId()).isEqualTo("source-event-1");
            assertThat(value.orderId()).isNull();
            assertThat(value.causationId()).isNull();
            assertThat(value.metadata()).doesNotContainKey("customerId").doesNotContainKey("customerType");
        });
        assertThat(values.get(0).eventId()).isEqualTo("source-event-1");
        assertThat(values.get(3).eventId()).isEqualTo("ledger-event-1");
    }
}
