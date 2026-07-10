package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.audit.AuditAction;
import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.economy.model.LogisticsCostEventEntity;
import com.csj.archive.logistics.economy.model.LogisticsCostType;
import com.csj.archive.logistics.economy.model.LogisticsProfitSnapshotEntity;
import com.csj.archive.logistics.economy.model.LogisticsRevenueEventEntity;
import com.csj.archive.logistics.economy.model.LogisticsRevenueType;
import com.csj.archive.logistics.economy.properties.LogisticsEconomyProperties;
import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.route.RouteCostEntity;
import com.csj.archive.logistics.route.RoutePlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.csj.archive.logistics.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

@Service
public class LogisticsEconomyService {
    private static final String DEFAULT_SOURCE = "Archive-Logistics";
    private static final String BILLING_TARGET_NEXUS = "Archive-Nexus";
    private static final String LEDGER_TARGET = "Archive-Ledger";
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final LogisticsRevenueEventRepository revenueEventRepository;
    private final LogisticsCostEventRepository costEventRepository;
    private final LogisticsProfitSnapshotRepository snapshotRepository;
    private final AuditLogService auditLogService;
    private final LogisticsOutboxRepository outboxRepository;
    private final LogisticsEconomyProperties properties;
    private final com.csj.archive.logistics.common.IdGenerator idGenerator;
    private final Clock clock;

    public LogisticsEconomyService(
            LogisticsRevenueEventRepository revenueEventRepository,
            LogisticsCostEventRepository costEventRepository,
            LogisticsProfitSnapshotRepository snapshotRepository,
            AuditLogService auditLogService,
            LogisticsOutboxRepository outboxRepository,
            LogisticsEconomyProperties properties,
            com.csj.archive.logistics.common.IdGenerator idGenerator,
            Clock clock
    ) {
        this.revenueEventRepository = revenueEventRepository;
        this.costEventRepository = costEventRepository;
        this.snapshotRepository = snapshotRepository;
        this.auditLogService = auditLogService;
        this.outboxRepository = outboxRepository;
        this.properties = properties;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public RouteEconomyResult createRouteEconomyEvents(RoutePlan routePlan,
                                                       RouteCostEntity routeCost,
                                                       String correlationId,
                                                       String causationId,
                                                       String simulationRunId,
                                                       String settlementCycleId) {
        if (!properties.isEnabled() || routePlan == null || routeCost == null) {
            return RouteEconomyResult.from(routePlan, routeCost, properties);
        }

        RouteEconomyResult accounting = RouteEconomyResult.from(routePlan, routeCost, properties);
        LocalDateTime now = LocalDateTime.now(clock);
        String routePlanId = routePlan.routePlanId();
        String safeCorrelationId = hasText(correlationId) ? correlationId : routePlanId;
        String safeCausationId = hasText(causationId) ? causationId : routePlanId;

        createRevenue(
                routePlanId,
                safeCorrelationId,
                safeCausationId,
                simulationRunId,
                settlementCycleId,
                LogisticsRevenueType.LOGISTICS_DELIVERY_FEE_EARNED,
                accounting.deliveryFee(),
                accounting.deliveryFee(),
                "Delivery fee for deterministic route plan: " + routePlanId,
                now
        );
        if (routePlan.cost().urgentSurcharge() > 0) {
            createRevenue(
                    routePlanId,
                    safeCorrelationId,
                    safeCausationId,
                    simulationRunId,
                    settlementCycleId,
                    LogisticsRevenueType.LOGISTICS_URGENT_SURCHARGE_EARNED,
                    accounting.urgentSurcharge(),
                    accounting.urgentSurcharge(),
                    "Urgent surcharge from priority policy.",
                    now
            );
        }
        if (routePlan.requiresColdChain()) {
            createRevenue(
                    routePlanId,
                    safeCorrelationId,
                    safeCausationId,
                    simulationRunId,
                    settlementCycleId,
                    LogisticsRevenueType.LOGISTICS_COLD_CHAIN_SURCHARGE_EARNED,
                    accounting.coldChainSurcharge(),
                    accounting.coldChainSurcharge(),
                    "Cold-chain surcharge for synthetic route policy.",
                    now
            );
        }
        if (routePlan.deviated()) {
            createRevenue(
                    routePlanId,
                    safeCorrelationId,
                    safeCausationId,
                    simulationRunId,
                    settlementCycleId,
                    LogisticsRevenueType.LOGISTICS_ROUTE_DEVIATION_SURCHARGE_EARNED,
                    accounting.deviationSurcharge(),
                    accounting.deviationSurcharge(),
                    "Deviation surcharge for synthetic route risk.",
                    now
            );
        }
        if (routePlan.delayed()) {
            createRevenue(
                    routePlanId,
                    safeCorrelationId,
                    safeCausationId,
                    simulationRunId,
                    settlementCycleId,
                    LogisticsRevenueType.LOGISTICS_DELAY_PENALTY_RECHARGED,
                    accounting.delayPenaltyRecharged(),
                    accounting.delayPenaltyRecharged(),
                    "Re-charged delay penalty for synthetic route.",
                    now
            );
        }

        createCost(
                routePlanId,
                safeCorrelationId,
                safeCausationId,
                simulationRunId,
                settlementCycleId,
                LogisticsCostType.LOGISTICS_FUEL_COST_INCURRED,
                accounting.fuelCost(),
                "Fuel cost incurred for synthetic transportation.",
                now
        );
        createCost(
                routePlanId,
                safeCorrelationId,
                safeCausationId,
                simulationRunId,
                settlementCycleId,
                LogisticsCostType.LOGISTICS_TOLL_COST_INCURRED,
                accounting.tollCost(),
                "Toll cost incurred for synthetic route.",
                now
        );
        createCost(
                routePlanId,
                safeCorrelationId,
                safeCausationId,
                simulationRunId,
                settlementCycleId,
                LogisticsCostType.LOGISTICS_DELAY_PENALTY_COST_INCURRED,
                accounting.delayPenaltyCost(),
                "Synthetic delay penalty cost.",
                now
        );
        createCost(
                routePlanId,
                safeCorrelationId,
                safeCausationId,
                simulationRunId,
                settlementCycleId,
                LogisticsCostType.LOGISTICS_COLD_CHAIN_RISK_COST_INCURRED,
                accounting.coldChainRiskCost(),
                "Cold chain risk penalty cost.",
                now
        );
        createCost(
                routePlanId,
                safeCorrelationId,
                safeCausationId,
                simulationRunId,
                settlementCycleId,
                LogisticsCostType.LOGISTICS_OPERATION_COST_INCURRED,
                properties.getOperationCostPerRoute(),
                "Operation cost for synthetic processing.",
                now
        );

        if (routePlan.requiresColdChain() && routePlan.delayed() && routeCost.coldChainPenalty() > 0) {
            createCost(
                    routePlanId,
                    safeCorrelationId,
                    safeCausationId,
                    simulationRunId,
                    settlementCycleId,
                    LogisticsCostType.LOGISTICS_COLD_CHAIN_RISK_COST_INCURRED,
                    routeCost.coldChainPenalty(),
                    "Cold-chain delayed risk cost.",
                    now
            );
        }

        recordSnapshot(now.toLocalDate(), "daily accounting auto snapshot after route accounting", now);
        return accounting;
    }

    @Transactional
    public void recordLedgerPublishFeeEvents(LogisticsOutboxEntity outbox) {
        if (!properties.isEnabled() || outbox == null || isLedgerFeeEventType(outbox.eventType())) {
            return;
        }
        JsonNode payload = outbox.payload();
        LocalDateTime now = LocalDateTime.now(clock);
        String correlationId = text(payload, "correlationId", outbox.eventId());
        String causationId = text(payload, "causationId", outbox.eventId());
        String settlementCycleId = text(payload, "settlementCycleId", null);
        String simulationRunId = text(payload, "simulationRunId", null);
        long agencyFee = properties.getLedgerSettlementAgencyFeePerEvent();
        long reconcileFee = properties.getLedgerReconciliationFee();

        if (agencyFee > 0) {
            createCost(
                    outbox.eventId(),
                    correlationId,
                    causationId,
                    simulationRunId,
                    settlementCycleId,
                    LogisticsCostType.LEDGER_SETTLEMENT_AGENCY_FEE_PAID,
                    agencyFee,
                    "Ledger settlement agency fee paid for one synthetic publish.",
                    now
            );
        }
        if (reconcileFee > 0) {
            createCost(
                    outbox.eventId(),
                    correlationId,
                    causationId,
                    simulationRunId,
                    settlementCycleId,
                    LogisticsCostType.LEDGER_RECONCILIATION_FEE_PAID,
                    reconcileFee,
                    "Ledger reconciliation fee for synthetic publish.",
                    now
            );
        }
        recordSnapshot(now.toLocalDate(), "snapshot after ledger publish fee recording: " + outbox.eventId(), now);
    }

    @Transactional
    public LogisticsEconomySummaryResponse summary() {
        long revenue = safe(revenueEventRepository.sumRevenue());
        long cost = safe(costEventRepository.sumCost());
        LogisticsProfitSnapshotEntity latest = snapshotRepository.findTopByOrderByCreatedAtDesc().orElse(null);
        return LogisticsEconomySummaryResponse.from(latest, revenue, cost);
    }

    @Transactional(readOnly = true)
    public PageResponse<LogisticsRevenueEventEntity> revenueEvents(
            Pageable pageable,
            String billedToService,
            String settlementCycleId,
            String sourceService,
            String revenueType
    ) {
        Page<LogisticsRevenueEventEntity> page = resolveRevenuePage(pageable, billedToService, settlementCycleId, sourceService, revenueType);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<LogisticsCostEventEntity> costEvents(
            Pageable pageable,
            String paidToService,
            String settlementCycleId,
            String sourceService,
            String costType
    ) {
        Page<LogisticsCostEventEntity> page = resolveCostPage(pageable, paidToService, settlementCycleId, sourceService, costType);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<LogisticsProfitSnapshotResponse> snapshots(LocalDate settlementDate,
                                                                 Pageable pageable) {
        Page<LogisticsProfitSnapshotEntity> page = settlementDate == null
                ? snapshotRepository.findAll(pageable)
                : snapshotRepository.findBySettlementDate(settlementDate, pageable);
        return PageResponse.from(page.map(entity ->
                new LogisticsProfitSnapshotResponse(
                        entity.snapshotId(),
                        entity.settlementDate(),
                        entity.revenueAmount(),
                        entity.costAmount(),
                        entity.profitAmount(),
                        entity.cashBalance(),
                        entity.bankruptcyRisk(),
                        entity.createdAt()
                )));
    }

    public RouteEconomyResult accounting(RoutePlan routePlan, RouteCostEntity routeCost) {
        return RouteEconomyResult.from(routePlan, routeCost, properties);
    }

    public int maxHop() {
        return properties.getMaxHop();
    }

    @Transactional
    public void recordWorkforceCost(String aggregateId,
                                    String correlationId,
                                    String causationId,
                                    String simulationRunId,
                                    String settlementCycleId,
                                    LogisticsCostType type,
                                    long costAmount,
                                    String reason,
                                    LocalDateTime now) {
        if (!properties.isEnabled()) {
            return;
        }
        createCost(
                aggregateId,
                correlationId,
                causationId,
                simulationRunId,
                settlementCycleId,
                type,
                costAmount,
                reason,
                now
        );
        recordSnapshot(now.toLocalDate(), "snapshot after workforce cost recording: " + aggregateId, now);
    }

    private Page<LogisticsRevenueEventEntity> resolveRevenuePage(
            Pageable pageable,
            String billedToService,
            String settlementCycleId,
            String sourceService,
            String revenueType
    ) {
        if (hasText(sourceService)) {
            List<LogisticsRevenueEventEntity> filtered = revenueEventRepository.findAll().stream()
                    .filter(event -> sourceService.equals(event.sourceService()))
                    .toList();
            return pageify(filtered, pageable);
        }
        if (hasText(settlementCycleId)) {
            return revenueEventRepository.findBySettlementCycleId(settlementCycleId, pageable);
        }
        if (hasText(billedToService)) {
            return revenueEventRepository.findByBilledToService(billedToService, pageable);
        }
        if (hasText(revenueType)) {
            try {
                return revenueEventRepository.findByRevenueType(LogisticsRevenueType.valueOf(revenueType), pageable);
            } catch (IllegalArgumentException ignored) {
                return revenueEventRepository.findAll(pageable);
            }
        }
        return revenueEventRepository.findAll(pageable);
    }

    private Page<LogisticsCostEventEntity> resolveCostPage(
            Pageable pageable,
            String paidToService,
            String settlementCycleId,
            String sourceService,
            String costType
    ) {
        if (hasText(sourceService)) {
            List<LogisticsCostEventEntity> filtered = costEventRepository.findAll().stream()
                    .filter(event -> sourceService.equals(event.sourceService()))
                    .toList();
            return pageifyCosts(filtered, pageable);
        }
        if (hasText(settlementCycleId)) {
            return costEventRepository.findBySettlementCycleId(settlementCycleId, pageable);
        }
        if (hasText(paidToService)) {
            return costEventRepository.findByPaidToService(paidToService, pageable);
        }
        if (hasText(costType)) {
            try {
                return costEventRepository.findByCostType(LogisticsCostType.valueOf(costType), pageable);
            } catch (IllegalArgumentException ignored) {
                return costEventRepository.findAll(pageable);
            }
        }
        return costEventRepository.findAll(pageable);
    }

    private Page<LogisticsRevenueEventEntity> pageify(List<LogisticsRevenueEventEntity> items, Pageable pageable) {
        int start = (int) Math.min(Integer.MAX_VALUE, pageable.getOffset());
        int end = Math.min(items.size(), start + pageable.getPageSize());
        List<LogisticsRevenueEventEntity> content = items.subList(Math.min(start, items.size()), end);
        return new PageImpl<>(content, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()), items.size());
    }

    private Page<LogisticsCostEventEntity> pageifyCosts(List<LogisticsCostEventEntity> items, Pageable pageable) {
        int start = (int) Math.min(Integer.MAX_VALUE, pageable.getOffset());
        int end = Math.min(items.size(), start + pageable.getPageSize());
        List<LogisticsCostEventEntity> content = items.subList(Math.min(start, items.size()), end);
        return new PageImpl<>(content, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()), items.size());
    }

    private void createRevenue(
            String aggregateId,
            String correlationId,
            String causationId,
            String simulationRunId,
            String settlementCycleId,
            LogisticsRevenueType type,
            long baseAmount,
            long revenueAmount,
            String reason,
            LocalDateTime now
    ) {
        if (revenueAmount <= 0) {
            return;
        }
        String eventId = idGenerator.logiticsEventId(type.name(), aggregateId);
        String idempotencyKey = "LOGISTICS:" + type.name() + ":" + aggregateId;
        if (revenueEventRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }
        revenueEventRepository.save(new LogisticsRevenueEventEntity(
                eventId,
                idempotencyKey,
                simulationRunId,
                settlementCycleId,
                DEFAULT_SOURCE,
                BILLING_TARGET_NEXUS,
                type,
                baseAmount,
                revenueAmount,
                properties.getDefaultCurrency(),
                reason,
                now
        ));
        auditLogService.record(
                AuditAction.OUTBOX_EVENT_CREATED,
                "logistics_revenue_event",
                eventId,
                null,
                type.name(),
                payloadMetadata(correlationId, causationId, aggregateId, simulationRunId, settlementCycleId, revenueAmount)
        );
    }

    private void createCost(
            String aggregateId,
            String correlationId,
            String causationId,
            String simulationRunId,
            String settlementCycleId,
            LogisticsCostType type,
            long costAmount,
            String reason,
            LocalDateTime now
    ) {
        if (costAmount <= 0) {
            return;
        }
        String eventId = idGenerator.logiticsEventId(type.name(), aggregateId);
        String idempotencyKey = "LOGISTICS:" + type.name() + ":" + aggregateId;
        if (costEventRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }
        costEventRepository.save(new LogisticsCostEventEntity(
                eventId,
                idempotencyKey,
                simulationRunId,
                settlementCycleId,
                DEFAULT_SOURCE,
                LEDGER_TARGET,
                type,
                costAmount,
                properties.getDefaultCurrency(),
                reason,
                now
        ));
        auditLogService.record(
                AuditAction.OUTBOX_EVENT_CREATED,
                "logistics_cost_event",
                eventId,
                null,
                type.name(),
                payloadMetadata(correlationId, causationId, aggregateId, simulationRunId, settlementCycleId, costAmount)
        );
    }

    public void recordSnapshot(LocalDate settlementDate, String reason, LocalDateTime now) {
        long totalRevenue = safe(revenueEventRepository.sumRevenue());
        long totalCost = safe(costEventRepository.sumCost());
        long profit = totalRevenue - totalCost;
        long previousBalance = snapshotRepository.findTopByOrderByCreatedAtDesc()
                .map(LogisticsProfitSnapshotEntity::cashBalance)
                .orElse(properties.getOpeningCashBalance());
        long nextBalance = previousBalance + profit;
        String snapshotId = "SNAP-" + settlementDate.format(BASIC_DATE) + "-" + idGenerator.shortHash(reason + ":" + settlementDate);
        String bankruptcyRisk = bankruptcyRisk(nextBalance, profit);
        snapshotRepository.save(new LogisticsProfitSnapshotEntity(
                snapshotId,
                settlementDate,
                totalRevenue,
                totalCost,
                profit,
                nextBalance,
                bankruptcyRisk,
                now
        ));
    }

    public LogisticsProfitSnapshotEntity currentSnapshot() {
        return snapshotRepository.findTopByOrderByCreatedAtDesc().orElse(null);
    }

    public String bankruptcyRisk(long cashBalance, long monthlyProfit) {
        BigDecimal balanceRate = cashBalance <= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(cashBalance)
                .divide(BigDecimal.valueOf(Math.max(1L, properties.getOpeningCashBalance())), 4, RoundingMode.HALF_UP);
        if (cashBalance < 0) {
            return "CRITICAL";
        }
        if (cashBalance < 500_000L || monthlyProfit < 0) {
            return "HIGH";
        }
        if (balanceRate.compareTo(BigDecimal.valueOf(0.35)) < 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Map<String, Object> payloadMetadata(String correlationId, String causationId, String aggregateId,
                                              String simulationRunId, String settlementCycleId, long amount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("aggregateId", aggregateId);
        payload.put("correlationId", correlationId);
        payload.put("causationId", causationId);
        payload.put("simulationRunId", simulationRunId);
        payload.put("settlementCycleId", settlementCycleId);
        payload.put("amount", amount);
        return payload;
    }

    private String text(JsonNode payload, String name, String fallback) {
        if (payload == null || !payload.hasNonNull(name)) {
            return fallback;
        }
        return payload.path(name).asText(fallback);
    }

    private boolean isLedgerFeeEventType(String eventType) {
        return eventType != null &&
                (eventType.startsWith("LEDGER_") && eventType.contains("FEE")) ||
                "LOGISTICS_DAILY_SETTLEMENT_FEE_EARNED".equals(eventType);
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    public record RouteEconomyResult(
            long deliveryFee,
            long urgentSurcharge,
            long coldChainSurcharge,
            long deviationSurcharge,
            long delayPenaltyRecharged,
            long delayPenaltyCost,
            long coldChainRiskCost,
            long operationCost,
            long totalRevenue,
            long totalCost,
            long netProfit,
            long fuelCost,
            long tollCost
    ) {
        public static RouteEconomyResult from(RoutePlan routePlan, RouteCostEntity routeCost, LogisticsEconomyProperties properties) {
            if (routePlan == null || routeCost == null) {
                return new RouteEconomyResult(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            }
            long deliveryFee = Math.max(
                    0L,
                    routeCost.totalCost()
                            - routeCost.urgentSurcharge()
                            - routeCost.delayPenalty()
                            - routeCost.coldChainPenalty()
            );
            long urgentSurcharge = routeCost.urgentSurcharge();
            long coldChainSurcharge = routePlan.requiresColdChain() ? properties.getColdChainSurchargeRevenue() : 0L;
            long deviationSurcharge = routePlan.deviated() ? properties.getRouteDeviationSurcharge() : 0L;
            long delayPenaltyRecharged = routePlan.delayed() ? routeCost.delayPenalty() : 0L;
            long delayPenaltyCost = routePlan.delayed() ? routeCost.delayPenalty() : 0L;
            long coldChainRiskCost = routePlan.requiresColdChain() && routePlan.delayed() ? routeCost.coldChainPenalty() : 0L;
            long operationCost = properties.getOperationCostPerRoute();
            long totalRevenue = deliveryFee + urgentSurcharge + coldChainSurcharge + deviationSurcharge + delayPenaltyRecharged;
            long totalCost = routeCost.fuelCost() + routeCost.tollCost() + delayPenaltyCost + coldChainRiskCost + operationCost;
            long netProfit = totalRevenue - totalCost;
            return new RouteEconomyResult(
                    deliveryFee,
                    urgentSurcharge,
                    coldChainSurcharge,
                    deviationSurcharge,
                    delayPenaltyRecharged,
                    delayPenaltyCost,
                    coldChainRiskCost,
                    operationCost,
                    totalRevenue,
                    totalCost,
                    netProfit,
                    routeCost.fuelCost(),
                    routeCost.tollCost()
            );
        }
    }
}
