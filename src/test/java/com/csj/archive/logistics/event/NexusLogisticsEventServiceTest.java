package com.csj.archive.logistics.event;

import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.economy.LogisticsEconomyService;
import com.csj.archive.logistics.ledger.LedgerPublishProperties;
import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.csj.archive.logistics.route.RouteCostEntity;
import com.csj.archive.logistics.route.RoutePlan;
import com.csj.archive.logistics.route.RoutePlanEntity;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.csj.archive.logistics.workforce.WorkforceService;
import com.csj.archive.logistics.workforce.WorkforceSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

import org.mockito.ArgumentCaptor;

class NexusLogisticsEventServiceTest {
    private final NexusEventRepository nexusEventRepository = mock(NexusEventRepository.class);
    private final RoutePlanRepository routePlanRepository = mock(RoutePlanRepository.class);
    private final com.csj.archive.logistics.route.RouteCostRepository routeCostRepository = mock(com.csj.archive.logistics.route.RouteCostRepository.class);
    private final com.csj.archive.logistics.outbox.LogisticsOutboxRepository outboxRepository = mock(com.csj.archive.logistics.outbox.LogisticsOutboxRepository.class);
    private final com.csj.archive.logistics.route.SyntheticRouteCalculator routeCalculator = mock(com.csj.archive.logistics.route.SyntheticRouteCalculator.class);
    private final LedgerPublishProperties ledgerProperties = new LedgerPublishProperties();
    private final LogisticsEconomyService economyService = mock(LogisticsEconomyService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final WorkforceService workforceService = mock(WorkforceService.class);
    private final com.csj.archive.logistics.common.IdGenerator idGenerator = new com.csj.archive.logistics.common.IdGenerator(
            Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC),
            new com.csj.archive.logistics.common.DeterministicHash()
    );
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private TransactionTemplate transactionTemplate;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);
    private NexusLogisticsEventService service;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(mock(TransactionStatus.class));
        doNothing().when(transactionManager).commit(any(TransactionStatus.class));
        doNothing().when(transactionManager).rollback(any(TransactionStatus.class));
        transactionTemplate = new TransactionTemplate(transactionManager);
        ledgerProperties.setEnabled(false);

        service = new NexusLogisticsEventService(
                nexusEventRepository,
                routePlanRepository,
                routeCostRepository,
                outboxRepository,
                routeCalculator,
                ledgerProperties,
                economyService,
                objectMapper,
                auditLogService,
                idGenerator,
                transactionTemplate,
                meterRegistry,
                clock,
                workforceService
        );
        when(workforceService.workforceSummary()).thenReturn(new WorkforceSummaryResponse(
                "Archive-Logistics",
                false,
                true,
                java.time.LocalDate.parse("2026-01-15"),
                "WORKDAY-20260115-TEST",
                null,
                2,
                4,
                1,
                302L,
                0L,
                302L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                1_450_000L,
                "PRODUCTIVITY_REPORTED",
                "NONE"
        ));
    }

    @Test
    void processWithMarketMetadataPreservesOrderMetadataInRoutePlanAndPayload() {
        NexusLogisticsEventRequest request = request("evt-metadata-preserve", "SHIP-001", "FAC-A", "DC-SEOUL-01", "HIGH",
                new MarketShipmentMetadata(
                        "ORD-001",
                        "CUST-001",
                        "VIP_CUSTOMER",
                        "battery-module",
                        1500L,
                        1800L,
                        1,
                        true,
                        "HIGH",
                        true,
                        null,
                        "SIM-1",
                        "CYCLE-1",
                        "CORR-1",
                        "CAUSE-1",
                        2,
                        5
                ));
        RoutePlan routePlan = route(request, true);
        when(routeCalculator.calculate(eq(request), any())).thenReturn(routePlan);
        when(routeCalculator.calculate(request)).thenReturn(routePlan);
        when(nexusEventRepository.findByEventId(request.eventId())).thenReturn(Optional.empty());
        when(nexusEventRepository.findByIdempotencyKey(request.idempotencyKey())).thenReturn(Optional.empty());
        when(routePlanRepository.save(any(RoutePlanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(routeCostRepository.save(any(RouteCostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any(LogisticsOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nexusEventRepository.save(any(NexusEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.findByAggregateId(anyString())).thenReturn(Optional.empty());
        when(economyService.createRouteEconomyEvents(any(), any(), any(), any(), any(), any()))
                .thenReturn(new LogisticsEconomyService.RouteEconomyResult(
                        100_000L,
                        30_000L,
                        0L,
                        0L,
                        50_000L,
                        50_000L,
                        0L,
                        10_000L,
                        180_000L,
                        160_000L,
                        20_000L,
                        routePlan.cost().fuelCost(),
                        routePlan.cost().tollCost()
                ));

        service.process(request);

        ArgumentCaptor<RoutePlanEntity> routeCaptor = ArgumentCaptor.forClass(RoutePlanEntity.class);
        ArgumentCaptor<LogisticsOutboxEntity> outboxCaptor = ArgumentCaptor.forClass(LogisticsOutboxEntity.class);
        verify(routePlanRepository).save(routeCaptor.capture());
        verify(routeCostRepository).save(any());
        verify(outboxRepository).save(outboxCaptor.capture());

        RoutePlanEntity savedRoute = routeCaptor.getValue();
        assertThat(savedRoute.orderId()).isEqualTo("ORD-001");
        assertThat(savedRoute.customerType()).isEqualTo("VIP_CUSTOMER");
        assertThat(savedRoute.marketPriority()).isEqualTo("HIGH");
        assertThat(savedRoute.riskLevel()).isEqualTo(1);
        assertThat(savedRoute.expressOrder()).isTrue();

        var payload = outboxCaptor.getValue().payload();
        assertThat(payload.path("orderId").asText()).isEqualTo("ORD-001");
        assertThat(payload.path("customerType").asText()).isEqualTo("VIP_CUSTOMER");
        assertThat(payload.path("correlationId").asText()).isEqualTo("CORR-1");
        assertThat(payload.path("marketPriority").asText()).isEqualTo("HIGH");
        assertThat(payload.path("workdayId").asText()).isEqualTo("WORKDAY-20260115-TEST");
        assertThat(payload.path("payrollCost").asLong()).isEqualTo(1_450_000L);
        assertThat(payload.path("bottleneckRole").asText()).isEqualTo("NONE");
    }

    @Test
    void processWithoutMarketMetadataStillCreatesRouteAndOutbox() {
        NexusLogisticsEventRequest request = request("evt-metadata-empty", "SHIP-002", "FAC-B", "DC-DAEJEON-01", "NORMAL", null);
        RoutePlan routePlan = route(request, false);
        when(routeCalculator.calculate(eq(request), any())).thenReturn(routePlan);
        when(routeCalculator.calculate(request)).thenReturn(routePlan);
        when(nexusEventRepository.findByEventId(request.eventId())).thenReturn(Optional.empty());
        when(nexusEventRepository.findByIdempotencyKey(request.idempotencyKey())).thenReturn(Optional.empty());
        when(routePlanRepository.save(any(RoutePlanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(routeCostRepository.save(any(RouteCostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any(LogisticsOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nexusEventRepository.save(any(NexusEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.findByAggregateId(anyString())).thenReturn(Optional.empty());
        when(economyService.createRouteEconomyEvents(any(), any(), any(), any(), any(), any()))
                .thenReturn(new LogisticsEconomyService.RouteEconomyResult(
                        90_000L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        10_000L,
                        90_000L,
                        100_000L,
                        -10_000L,
                        routePlan.cost().fuelCost(),
                        routePlan.cost().tollCost()
                ));

        service.process(request);

        ArgumentCaptor<RoutePlanEntity> routeCaptor = ArgumentCaptor.forClass(RoutePlanEntity.class);
        verify(routePlanRepository).save(routeCaptor.capture());
        assertThat(routeCaptor.getValue().orderId()).isNull();
    }

    private RoutePlan route(NexusLogisticsEventRequest request, boolean withMetadata) {
        String priority = request.payload().normalizedPriority();
        return new RoutePlan(
                "ROUTE-" + request.eventId(),
                request.eventId(),
                request.payload().shipmentId(),
                request.payload().factoryId(),
                request.payload().originCode(),
                request.payload().destinationCode(),
                "VENDOR-LOGISTICS-01",
                withMetadata ? request.payload().orderId() : null,
                withMetadata ? request.payload().customerId() : null,
                withMetadata ? request.payload().customerType() : null,
                withMetadata ? request.payload().productType() : null,
                withMetadata ? request.payload().orderAmount() : null,
                withMetadata ? request.payload().totalAmount() : null,
                withMetadata ? request.payload().normalizedPriority() : priority,
                withMetadata ? request.payload().riskLevel() : null,
                withMetadata ? request.payload().expressOrder() : null,
                withMetadata ? request.payload().customerType() != null : null,
                withMetadata ? request.payload().correlationId() : null,
                withMetadata ? request.payload().causationId() : null,
                withMetadata ? request.payload().simulationRunId() : null,
                withMetadata ? request.payload().settlementCycleId() : null,
                withMetadata ? request.payload().hopCount() : null,
                withMetadata ? request.payload().maxHop() : null,
                new java.math.BigDecimal("42.00"),
                60,
                priority,
                java.math.BigDecimal.valueOf(0.1234),
                false,
                false,
                request.payload().requiresColdChain(),
                "COST_CONFIRMED",
                new com.csj.archive.logistics.route.RouteCost(
                        60_900L,
                        2_520L,
                        priority.equals("HIGH") ? 30_000L : 0L,
                        0L,
                        0L,
                        60_900L + 2_520L + (priority.equals("HIGH") ? 30_000L : 0L),
                        "KRW",
                        false,
                        "synthetic"
                )
        );
    }

    private NexusLogisticsEventRequest request(String eventId, String shipmentId, String origin, String destination,
                                              String priority, MarketShipmentMetadata metadata) {
        return new NexusLogisticsEventRequest(
                eventId,
                "NEXUS:LOGISTICS_DISPATCHED:" + origin + ":" + shipmentId,
                "Archive-Nexus",
                "LOGISTICS_DISPATCHED",
                Instant.parse("2026-01-15T10:32:15Z"),
                new NexusLogisticsEventRequest.Payload(
                        origin,
                        shipmentId,
                        origin,
                        destination,
                        priority,
                        "battery-module",
                        120,
                        false,
                        metadata == null ? null : metadata.orderId(),
                        metadata == null ? null : metadata.customerId(),
                        metadata == null ? null : metadata.customerType(),
                        metadata == null ? null : metadata.productType(),
                        metadata == null ? null : metadata.orderAmount(),
                        metadata == null ? null : metadata.totalAmount(),
                        metadata == null ? null : metadata.riskLevel(),
                        metadata == null ? null : metadata.expressOrder(),
                        metadata == null ? null : metadata.riskTag(),
                        metadata == null ? null : metadata.vipCustomer(),
                        metadata == null ? null : metadata.simulationRunId(),
                        metadata == null ? null : metadata.settlementCycleId(),
                        metadata == null ? null : metadata.correlationId(),
                        metadata == null ? null : metadata.causationId(),
                        metadata == null ? null : metadata.hopCount(),
                        metadata == null ? null : metadata.maxHop(),
                        metadata == null ? null : metadata.marketPriority()
                )
        );
    }
}
