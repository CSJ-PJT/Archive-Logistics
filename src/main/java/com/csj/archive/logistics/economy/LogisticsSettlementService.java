package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.economy.model.LogisticsDailySettlementEntity;
import com.csj.archive.logistics.economy.model.LogisticsSettlementStatus;
import com.csj.archive.logistics.economy.properties.LogisticsEconomyProperties;
import com.csj.archive.logistics.common.PageResponse;
import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.csj.archive.logistics.outbox.LogisticsOutboxEvent;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.outbox.OutboxStatus;
import com.csj.archive.logistics.route.RouteCostEntity;
import com.csj.archive.logistics.route.RouteCostRepository;
import com.csj.archive.logistics.route.RoutePlan;
import com.csj.archive.logistics.route.RoutePlanEntity;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

@Service
public class LogisticsSettlementService {
    private static final String BILLING_TARGET_NEXUS = "Archive-Nexus";
    private static final String OUTBOX_SOURCE = "Archive-Logistics";

    private final LogisticsDailySettlementRepository settlementRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteCostRepository routeCostRepository;
    private final LogisticsOutboxRepository outboxRepository;
    private final LogisticsEconomyService economyService;
    private final LogisticsEconomyProperties properties;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public LogisticsSettlementService(LogisticsDailySettlementRepository settlementRepository,
                                     RoutePlanRepository routePlanRepository,
                                     RouteCostRepository routeCostRepository,
                                     LogisticsOutboxRepository outboxRepository,
                                     LogisticsEconomyService economyService,
                                     LogisticsEconomyProperties properties,
                                     IdGenerator idGenerator,
                                     ObjectMapper objectMapper,
                                     AuditLogService auditLogService,
                                     Clock clock) {
        this.settlementRepository = settlementRepository;
        this.routePlanRepository = routePlanRepository;
        this.routeCostRepository = routeCostRepository;
        this.outboxRepository = outboxRepository;
        this.economyService = economyService;
        this.properties = properties;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional
    public LogisticsDailySettlementRunResult run(LocalDate settlementDate, String factoryId) {
        LocalDate date = settlementDate == null ? LocalDate.now(clock) : settlementDate;
        List<String> factories = factoryId == null || factoryId.isBlank()
                ? routePlanRepository.findDistinctFactoryIdsByCreatedAtBetween(start(date), end(date))
                : List.of(factoryId);
        String cycleId = "LCYCLE-" + date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        List<LogisticsDailySettlementResponse> settlements = factories.stream()
                .map(factory -> runForFactory(date, factory, cycleId))
                .toList();
        return new LogisticsDailySettlementRunResult(
                date,
                settlements.size(),
                (int) settlements.stream().filter(s -> "SENT".equals(s.status()) || "DRY_RUN".equals(s.status())).count(),
                (int) settlements.stream().filter(s -> "SKIPPED".equals(s.status())).count(),
                0,
                settlements.size(),
                (int) settlements.stream().filter(s -> s.totalCost() > 0).count(),
                settlements
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<LogisticsDailySettlementResponse> settlements(LocalDate settlementDate, Pageable pageable) {
        Page<LogisticsDailySettlementEntity> page = settlementDate == null
                ? settlementRepository.findAll(pageable)
                : settlementRepository.findBySettledAt(settlementDate, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public LogisticsDailySettlementResponse settlement(String settlementId) {
        return settlementRepository.findBySettlementId(settlementId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public LogisticsDailySettlementSummaryResponse summary(LocalDate settlementDate) {
        List<LogisticsDailySettlementEntity> settlements = settlementDate == null
                ? settlementRepository.findAll()
                : settlementRepository.findBySettledAt(settlementDate);
        long totalRoutes = settlements.stream().mapToLong(LogisticsDailySettlementEntity::routeCount).sum();
        long totalDeliveryFee = settlements.stream().mapToLong(LogisticsDailySettlementEntity::totalDeliveryFee).sum();
        long totalSurcharge = settlements.stream().mapToLong(LogisticsDailySettlementEntity::totalSurcharge).sum();
        long totalCost = settlements.stream().mapToLong(LogisticsDailySettlementEntity::totalCost).sum();
        long ledgerFee = settlements.stream().mapToLong(LogisticsDailySettlementEntity::ledgerFeePaid).sum();
        long netProfit = settlements.stream().mapToLong(LogisticsDailySettlementEntity::netProfit).sum();
        BigDecimal rate = totalRoutes == 0
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(Math.max(0L, netProfit))
                .divide(BigDecimal.valueOf(totalRoutes), 4, RoundingMode.HALF_UP);
        return new LogisticsDailySettlementSummaryResponse(
                settlementDate,
                settlements.size(),
                totalRoutes,
                totalDeliveryFee,
                totalSurcharge,
                totalCost,
                ledgerFee,
                netProfit,
                rate
        );
    }

    private LogisticsDailySettlementResponse runForFactory(LocalDate date, String factoryId, String cycleId) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<RoutePlanEntity> plans = routePlanRepository.findByFactoryIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                factoryId,
                start(date),
                end(date)
        );
        Map<String, LogisticsOutboxEntity> outboxByPlan = outboxRepository.findByAggregateIdIn(plans.stream()
                        .map(RoutePlanEntity::routePlanId).toList())
                .stream()
                .collect(Collectors.toMap(LogisticsOutboxEntity::aggregateId, x -> x, (left, right) -> left));
        Map<String, RouteCostEntity> costByPlan = routeCostRepository.findByRoutePlanIdIn(plans.stream()
                        .map(RoutePlanEntity::routePlanId).toList())
                .stream()
                .collect(Collectors.toMap(RouteCostEntity::routePlanId, x -> x, (left, right) -> left));

        List<RoutePlanEntity> settledPlans = plans.stream()
                .filter(plan -> {
                    LogisticsOutboxEntity outbox = outboxByPlan.get(plan.routePlanId());
                    return outbox != null && outbox.status() == OutboxStatus.PUBLISHED;
                })
                .toList();

        if (settledPlans.isEmpty()) {
            String settlementId = settlementId(date, factoryId);
            return toResponse(settlementRepository
                    .findBySettlementId(settlementId)
                    .orElseGet(() -> settlementRepository.save(new LogisticsDailySettlementEntity(
                            settlementId,
                            cycleId,
                            date,
                            BILLING_TARGET_NEXUS,
                            factoryId,
                            0L,
                            0L,
                            0L,
                            0L,
                            0L,
                            0L,
                            LogisticsSettlementStatus.SKIPPED,
                            now
                    )))
            );
        }

        long routeCount = settledPlans.size();
        long totalDelivery = 0L;
        long totalSurcharge = 0L;
        long totalCost = 0L;

        for (RoutePlanEntity plan : settledPlans) {
            RouteCostEntity cost = costByPlan.get(plan.routePlanId());
            if (cost == null) {
                continue;
            }
            LogisticsEconomyService.RouteEconomyResult accounting = economics(plan, cost);
            totalDelivery += accounting.deliveryFee();
            totalSurcharge += accounting.urgentSurcharge()
                    + accounting.coldChainSurcharge()
                    + accounting.deviationSurcharge()
                    + accounting.delayPenaltyRecharged();
            totalCost += accounting.totalCost();
        }
        long ledgerFee = properties.isEnabled() ? (properties.getLedgerSettlementAgencyFeePerEvent() + properties.getLedgerReconciliationFee()) : 0L;
        long netProfit = totalDelivery + totalSurcharge - totalCost - ledgerFee;

        String settlementId = settlementId(date, factoryId);
        long finalTotalDelivery = totalDelivery;
        long finalTotalSurcharge = totalSurcharge;
        long finalTotalCost = totalCost;
        long finalLedgerFee = ledgerFee;
        long finalNetProfit = netProfit;
        LogisticsDailySettlementEntity entity = settlementRepository.findBySettlementId(settlementId)
                .orElseGet(() -> settlementRepository.save(new LogisticsDailySettlementEntity(
                        settlementId,
                        cycleId,
                        date,
                        BILLING_TARGET_NEXUS,
                        factoryId,
                        routeCount,
                        finalTotalDelivery,
                        finalTotalSurcharge,
                        finalTotalCost + finalLedgerFee,
                        finalLedgerFee,
                        finalNetProfit,
                        properties.isEnabled() ? LogisticsSettlementStatus.PENDING : LogisticsSettlementStatus.DRY_RUN,
                        now
                )));

        if (entity.routeCount() == 0) {
            entity = new LogisticsDailySettlementEntity(
                    settlementId,
                    cycleId,
                    date,
                    BILLING_TARGET_NEXUS,
                    factoryId,
                    routeCount,
                    totalDelivery,
                    totalSurcharge,
                    totalCost + ledgerFee,
                    ledgerFee,
                    netProfit,
                    properties.isEnabled() ? LogisticsSettlementStatus.PENDING : LogisticsSettlementStatus.DRY_RUN,
                    now
            );
            settlementRepository.save(entity);
        }
        if (properties.isEnabled() && entity.status() != LogisticsSettlementStatus.SENT) {
            createSettlementOutbox(eventId(settlementId), settlementId, factoryId, cycleId, routeCount, totalDelivery, totalSurcharge, totalCost, entity);
        }
        return toResponse(entity);
    }

    private String settlementId(LocalDate date, String factoryId) {
        return "LGS-SETTLE-" + date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-" + factoryId;
    }

    private String eventId(String settlementId) {
        return idGenerator.logiticsEventId("LOGISTICS_DAILY_SETTLEMENT_FEE_EARNED", settlementId);
    }

    private void createSettlementOutbox(String eventId,
                                       String settlementId,
                                       String factoryId,
                                       String cycleId,
                                       long routeCount,
                                       long totalDelivery,
                                       long totalSurcharge,
                                       long totalCost,
                                       LogisticsDailySettlementEntity entity) {
        if (outboxRepository.findByAggregateId(settlementId).isPresent()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceService", OUTBOX_SOURCE);
        payload.put("billedToService", BILLING_TARGET_NEXUS);
        payload.put("simulationRunId", null);
        payload.put("settlementCycleId", cycleId);
        payload.put("settlementId", settlementId);
        payload.put("settlementDate", entity.settledAt().toString());
        payload.put("factoryId", factoryId);
        payload.put("routeCount", routeCount);
        payload.put("totalDeliveryFee", totalDelivery);
        payload.put("totalSurcharge", totalSurcharge);
        payload.put("totalCost", totalCost);
        payload.put("ledgerFeePaid", properties.getLedgerSettlementAgencyFeePerEvent() + properties.getLedgerReconciliationFee());
        payload.put("logisticsRevenue", totalDelivery + totalSurcharge);
        payload.put("logisticsCost", totalCost + properties.getLedgerSettlementAgencyFeePerEvent() + properties.getLedgerReconciliationFee());
        payload.put("logisticsProfit", totalDelivery + totalSurcharge - totalCost
                - (properties.getLedgerSettlementAgencyFeePerEvent() + properties.getLedgerReconciliationFee()));
        payload.put("correlationId", settlementId);
        payload.put("causationId", settlementId);
        payload.put("hopCount", 0);
        payload.put("maxHop", 10);
        outboxRepository.save(new LogisticsOutboxEntity(
                new LogisticsOutboxEvent(
                        eventId,
                        "LOGISTICS:SETTLEMENT:" + settlementId,
                        OUTBOX_SOURCE,
                        "LOGISTICS_DAILY_SETTLEMENT_FEE_EARNED",
                        "LOGISTICS_SETTLEMENT",
                        settlementId,
                        objectMapper.valueToTree(payload)
                ),
                LocalDateTime.now(clock)
        ));
        auditLogService.record(
                com.csj.archive.logistics.audit.AuditAction.OUTBOX_EVENT_CREATED,
                "logistics_settlement",
                settlementId,
                null,
                "CREATED",
                payload
        );
    }

    private LogisticsEconomyService.RouteEconomyResult economics(RoutePlanEntity plan, RouteCostEntity cost) {
        RoutePlan routePlan = new RoutePlan(
                plan.routePlanId(),
                plan.sourceEventId(),
                plan.shipmentId(),
                plan.factoryId(),
                plan.originCode(),
                plan.destinationCode(),
                plan.vendorId(),
                plan.distanceKm(),
                plan.estimatedMinutes(),
                plan.priority(),
                plan.riskScore(),
                plan.delayed(),
                plan.deviated(),
                plan.requiresColdChain(),
                plan.routeStatus(),
                null
        );
        return economyService.accounting(routePlan, cost);
    }

    private LogisticsDailySettlementResponse toResponse(LogisticsDailySettlementEntity entity) {
        return new LogisticsDailySettlementResponse(
                entity.settlementId(),
                entity.settlementCycleId(),
                entity.settledAt(),
                entity.billedToService(),
                entity.factoryId(),
                entity.routeCount(),
                entity.totalDeliveryFee(),
                entity.totalSurcharge(),
                entity.totalCost(),
                entity.ledgerFeePaid(),
                entity.netProfit(),
                entity.status().name(),
                entity.completedAt()
        );
    }

    private LocalDateTime start(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime end(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }
}
