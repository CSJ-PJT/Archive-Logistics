package com.csj.archive.logistics.runtime;

import com.csj.archive.logistics.common.DeterministicHash;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.event.NexusLogisticsEventRequest;
import com.csj.archive.logistics.route.RouteCost;
import com.csj.archive.logistics.route.RoutePlan;
import com.csj.archive.logistics.route.RoutePlanEntity;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.csj.archive.logistics.workforce.WorkdayProductivityResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipmentLifecycleServiceTest {
    private final RoutePlanRepository routePlanRepository = mock(RoutePlanRepository.class);
    private final ShipmentRuntimeEventRepository eventRepository = mock(ShipmentRuntimeEventRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-11T10:00:00Z"), ZoneOffset.UTC);
    private final ShipmentLifecycleService service = new ShipmentLifecycleService(
            routePlanRepository, eventRepository, new ObjectMapper(), new IdGenerator(clock, new DeterministicHash()), clock
    );

    @Test
    void lifecyclePersistsCreatedAssignedTransitAndCompletedExactlyOnce() {
        RoutePlanEntity route = route(false, false);
        NexusLogisticsEventRequest request = request(route.sourceEventId());
        when(routePlanRepository.findBySourceEventIdIn(any())).thenReturn(List.of(route));
        when(eventRepository.existsByRoutePlanIdAndEventType(anyString(), anyString())).thenReturn(false);

        ShipmentLifecycleService.ShipmentLifecycleResult result = service.advance(List.of(request), workday(1));

        ArgumentCaptor<ShipmentRuntimeEventEntity> captor = ArgumentCaptor.forClass(ShipmentRuntimeEventEntity.class);
        verify(eventRepository, times(7)).save(captor.capture());
        assertThat(result.completed()).isEqualTo(1);
        assertThat(result.delayed()).isZero();
        assertThat(route.routeStatus()).isEqualTo("DELIVERY_COMPLETED");
        assertThat(captor.getAllValues()).extracting(ShipmentRuntimeEventEntity::eventType)
                .containsExactly("SHIPMENT_CREATED", "ROUTE_ASSIGNED", "ROUTE_COST_CALCULATED", "LOGISTICS_COST_CONFIRMED",
                        "TRUCK_DISPATCHED", "DELIVERY_IN_TRANSIT", "DELIVERY_COMPLETED");

        service.advance(List.of(request), workday(1));
        verify(eventRepository, times(7)).save(any());
    }

    @Test
    void zeroCapacityDelaysColdChainShipmentAndEmitsRiskEvent() {
        RoutePlanEntity route = route(true, false);
        when(routePlanRepository.findBySourceEventIdIn(any())).thenReturn(List.of(route));
        when(eventRepository.existsByRoutePlanIdAndEventType(anyString(), anyString())).thenReturn(false);

        ShipmentLifecycleService.ShipmentLifecycleResult result = service.advance(List.of(request(route.sourceEventId())), workday(0));

        ArgumentCaptor<ShipmentRuntimeEventEntity> captor = ArgumentCaptor.forClass(ShipmentRuntimeEventEntity.class);
        verify(eventRepository, times(6)).save(captor.capture());
        assertThat(result.completed()).isZero();
        assertThat(result.delayed()).isEqualTo(1);
        assertThat(route.routeStatus()).isEqualTo("DELIVERY_DELAYED");
        assertThat(captor.getAllValues()).extracting(ShipmentRuntimeEventEntity::eventType)
                .contains("DELIVERY_DELAYED", "COLD_CHAIN_RISK_DETECTED");
    }

    private RoutePlanEntity route(boolean coldChain, boolean delayed) {
        RoutePlan route = new RoutePlan(
                "ROUTE-1", "evt-nexus-1", "SHIP-1", "FAC-A", "FAC-A", "DC-SEOUL-01", "VENDOR-LOGISTICS-01",
                "ORD-1", null, "SYNTHETIC_CUSTOMER", "battery-module", null, null, "HIGH", null, false, false,
                "CORR-1", "CAUSE-1", "SIM-1", "CYCLE-1", 0, 5, new BigDecimal("42.00"), 60,
                "HIGH", new BigDecimal(delayed ? "0.8000" : "0.1200"), delayed, false, coldChain, "ROUTE_ASSIGNED",
                new RouteCost(60_900L, 2_520L, 30_000L, 0L, 0L, 93_420L, "KRW", false, "synthetic")
        );
        return new RoutePlanEntity(route, java.time.LocalDateTime.now(clock));
    }

    private NexusLogisticsEventRequest request(String eventId) {
        return new NexusLogisticsEventRequest(eventId, "NEXUS:SHIP-1", "Archive-Nexus", "LOGISTICS_DISPATCHED",
                Instant.parse("2026-07-11T10:00:00Z"), new NexusLogisticsEventRequest.Payload(
                "FAC-A", "SHIP-1", "FAC-A", "DC-SEOUL-01", "HIGH", "battery-module", 1, false,
                "ORD-1", null, "SYNTHETIC_CUSTOMER", "battery-module", null, null, null, false,
                null, false, "SIM-1", "CYCLE-1", "CORR-1", "CAUSE-1", 0, 5, "HIGH"
        ));
    }

    private WorkdayProductivityResult workday(long completed) {
        return new WorkdayProductivityResult("WORKDAY-1", null, LocalDate.parse("2026-07-11"), false, true,
                1, 1, 1, 1, 10, completed, 10 - completed, 1, completed, 0, 1, completed, completed,
                0, 0, 0, BigDecimal.ONE, BigDecimal.ONE, 0L, "PRODUCTIVITY_REPORTED", "NONE");
    }
}
