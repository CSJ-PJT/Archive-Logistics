package com.csj.archive.logistics.settlement;

import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.event.NexusEventEntity;
import com.csj.archive.logistics.event.NexusEventRepository;
import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.csj.archive.logistics.outbox.LogisticsOutboxEvent;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.route.RouteCost;
import com.csj.archive.logistics.route.RouteCostEntity;
import com.csj.archive.logistics.route.RouteCostRepository;
import com.csj.archive.logistics.route.RoutePlan;
import com.csj.archive.logistics.route.RoutePlanEntity;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NexusDailySettlementServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NexusDailySettlementRepository settlementRepository = mock(NexusDailySettlementRepository.class);
    private final RoutePlanRepository routePlanRepository = mock(RoutePlanRepository.class);
    private final RouteCostRepository routeCostRepository = mock(RouteCostRepository.class);
    private final LogisticsOutboxRepository outboxRepository = mock(LogisticsOutboxRepository.class);
    private final NexusEventRepository nexusEventRepository = mock(NexusEventRepository.class);
    private final NexusDailySettlementClient nexusClient = mock(NexusDailySettlementClient.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-09T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void runPublishesManufacturingSettlementToNexus() {
        NexusSettlementProperties properties = new NexusSettlementProperties();
        properties.setEnabled(true);
        properties.setManufacturingShareRate(new BigDecimal("0.3000"));
        NexusDailySettlementService service = new NexusDailySettlementService(
                settlementRepository,
                routePlanRepository,
                routeCostRepository,
                outboxRepository,
                nexusEventRepository,
                nexusClient,
                properties,
                auditLogService,
                clock
        );
        LocalDate date = LocalDate.of(2026, 7, 9);
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 10, 0);
        RoutePlanEntity plan = new RoutePlanEntity(new RoutePlan(
                "ROUTE-1",
                "evt-nexus-1",
                "SHIP-1",
                "FAC-A",
                "FAC-A",
                "DC-SEOUL-01",
                "VENDOR-LOGISTICS-01",
                new BigDecimal("42.00"),
                67,
                "NORMAL",
                new BigDecimal("0.1200"),
                false,
                false,
                false,
                "COST_CONFIRMED",
                new RouteCost(60_900, 2_520, 0, 0, 0, 100_000, "KRW", false, "test")
        ), now);
        RouteCostEntity cost = new RouteCostEntity("ROUTE-1",
                new RouteCost(60_900, 2_520, 0, 0, 0, 100_000, "KRW", false, "test"),
                now);
        NexusEventEntity event = new NexusEventEntity(
                "evt-nexus-1",
                "NEXUS:LOGISTICS_DISPATCHED:FAC-A:SHIP-1",
                "Archive-Nexus",
                "LOGISTICS_DISPATCHED",
                objectMapper.createObjectNode().put("quantity", 120),
                now
        );
        LogisticsOutboxEntity outbox = new LogisticsOutboxEntity(new LogisticsOutboxEvent(
                "evt-logistics-1",
                "LOGISTICS:LOGISTICS_COST_CONFIRMED:ROUTE-1",
                "Archive-Logistics",
                "LOGISTICS_COST_CONFIRMED",
                "ROUTE_PLAN",
                "ROUTE-1",
                objectMapper.createObjectNode()
        ), now);
        outbox.markPublished(now);

        when(routePlanRepository.findDistinctFactoryIdsByCreatedAtBetween(any(), any())).thenReturn(List.of("FAC-A"));
        when(routePlanRepository.findByFactoryIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any(), any()))
                .thenReturn(List.of(plan));
        when(outboxRepository.findByAggregateIdIn(List.of("ROUTE-1"))).thenReturn(List.of(outbox));
        when(routeCostRepository.findByRoutePlanIdIn(List.of("ROUTE-1"))).thenReturn(List.of(cost));
        when(nexusEventRepository.findByEventIdIn(List.of("evt-nexus-1"))).thenReturn(List.of(event));
        when(settlementRepository.findBySettlementDateAndFactoryId(date, "FAC-A")).thenReturn(Optional.empty());
        when(settlementRepository.save(any(NexusDailySettlementEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nexusClient.publish(any())).thenReturn(objectMapper.createObjectNode().put("duplicate", false));

        NexusDailySettlementRunResult result = service.run(date, null);

        assertThat(result.requestedFactoryCount()).isEqualTo(1);
        assertThat(result.sentCount()).isEqualTo(1);
        assertThat(result.settlements()).hasSize(1);
        NexusDailySettlementResponse settlement = result.settlements().getFirst();
        assertThat(settlement.settlementId()).isEqualTo("LGS-SETTLE-20260709-FAC-A");
        assertThat(settlement.idempotencyKey()).isEqualTo("LOGISTICS:DAILY:2026-07-09:FAC-A");
        assertThat(settlement.totalShipments()).isEqualTo(1);
        assertThat(settlement.totalQuantity()).isEqualTo(120);
        assertThat(settlement.totalLogisticsCost()).isEqualTo(100_000);
        assertThat(settlement.manufacturingImpactCost()).isEqualTo(30_000);
        assertThat(settlement.status()).isEqualTo("SENT");

        ArgumentCaptor<NexusDailySettlementRequest> requestCaptor = ArgumentCaptor.forClass(NexusDailySettlementRequest.class);
        verify(nexusClient).publish(requestCaptor.capture());
        NexusDailySettlementRequest request = requestCaptor.getValue();
        assertThat(request.source()).isEqualTo("Archive-Logistics");
        assertThat(request.manufacturingImpactCost()).isEqualTo(30_000);
        assertThat(request.onTimeRate()).isEqualByComparingTo("1.0000");
        assertThat(request.payload()).containsEntry("syntheticData", true);
    }

    @Test
    void settlementCandidatesAreReadyOnlyAfterEveryOutboxRowIsPublished() {
        NexusDailySettlementService service = new NexusDailySettlementService(
                settlementRepository,
                routePlanRepository,
                routeCostRepository,
                outboxRepository,
                nexusEventRepository,
                nexusClient,
                new NexusSettlementProperties(),
                auditLogService,
                clock
        );
        LocalDate date = LocalDate.of(2026, 7, 9);
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 10, 0);
        RoutePlanEntity plan = new RoutePlanEntity(new RoutePlan(
                "ROUTE-READY",
                "evt-nexus-ready",
                "SHIP-READY",
                "FAC-A",
                "FAC-A",
                "DC-SEOUL-01",
                "VENDOR-LOGISTICS-01",
                new BigDecimal("42.00"),
                67,
                "NORMAL",
                new BigDecimal("0.1200"),
                false,
                false,
                false,
                "COST_CONFIRMED",
                new RouteCost(60_900, 2_520, 0, 0, 0, 100_000, "KRW", false, "test")
        ), now);
        LogisticsOutboxEntity outbox = new LogisticsOutboxEntity(new LogisticsOutboxEvent(
                "evt-logistics-ready",
                "LOGISTICS:LOGISTICS_COST_CONFIRMED:ROUTE-READY",
                "Archive-Logistics",
                "LOGISTICS_COST_CONFIRMED",
                "ROUTE_PLAN",
                "ROUTE-READY",
                objectMapper.createObjectNode()
        ), now);

        when(routePlanRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(List.of(plan));
        when(outboxRepository.findByAggregateIdIn(List.of("ROUTE-READY"))).thenReturn(List.of(outbox));

        assertThat(service.hasRunnableSettlementCandidates(date)).isFalse();

        outbox.markPublished(now);
        when(settlementRepository.findBySettlementDateAndFactoryId(date, "FAC-A")).thenReturn(Optional.empty());

        assertThat(service.hasRunnableSettlementCandidates(date)).isTrue();
    }
}
