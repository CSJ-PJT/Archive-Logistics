package com.csj.archive.logistics.settlement;

import com.csj.archive.logistics.audit.AuditAction;
import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.common.NotFoundException;
import com.csj.archive.logistics.common.PageResponse;
import com.csj.archive.logistics.event.NexusEventEntity;
import com.csj.archive.logistics.event.NexusEventRepository;
import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.outbox.OutboxStatus;
import com.csj.archive.logistics.route.RouteCostEntity;
import com.csj.archive.logistics.route.RouteCostRepository;
import com.csj.archive.logistics.route.RoutePlanEntity;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NexusDailySettlementService {
    private static final String SOURCE = "Archive-Logistics";
    private static final String CURRENCY = "KRW";
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final NexusDailySettlementRepository settlementRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteCostRepository routeCostRepository;
    private final LogisticsOutboxRepository outboxRepository;
    private final NexusEventRepository nexusEventRepository;
    private final NexusDailySettlementClient nexusClient;
    private final NexusSettlementProperties properties;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public NexusDailySettlementService(NexusDailySettlementRepository settlementRepository,
                                       RoutePlanRepository routePlanRepository,
                                       RouteCostRepository routeCostRepository,
                                       LogisticsOutboxRepository outboxRepository,
                                       NexusEventRepository nexusEventRepository,
                                       NexusDailySettlementClient nexusClient,
                                       NexusSettlementProperties properties,
                                       AuditLogService auditLogService,
                                       Clock clock) {
        this.settlementRepository = settlementRepository;
        this.routePlanRepository = routePlanRepository;
        this.routeCostRepository = routeCostRepository;
        this.outboxRepository = outboxRepository;
        this.nexusEventRepository = nexusEventRepository;
        this.nexusClient = nexusClient;
        this.properties = properties;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional
    public NexusDailySettlementRunResult run(LocalDate settlementDate, String factoryId) {
        LocalDate date = settlementDate == null ? LocalDate.now(clock) : settlementDate;
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<String> factories = blankToNull(factoryId) == null
                ? routePlanRepository.findDistinctFactoryIdsByCreatedAtBetween(start, end)
                : List.of(factoryId.trim());

        List<NexusDailySettlementResponse> results = factories.stream()
                .map(factory -> runFactory(date, factory))
                .toList();

        return new NexusDailySettlementRunResult(
                date,
                factories.size(),
                count(results, NexusDailySettlementStatus.SENT),
                count(results, NexusDailySettlementStatus.DRY_RUN),
                count(results, NexusDailySettlementStatus.RETRY),
                count(results, NexusDailySettlementStatus.FAILED),
                (int) results.stream().filter(result -> "SKIPPED".equals(result.status())).count(),
                results
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<NexusDailySettlementResponse> settlements(LocalDate date, Pageable pageable) {
        var page = date == null
                ? settlementRepository.findAll(pageable)
                : settlementRepository.findBySettlementDate(date, pageable);
        return PageResponse.from(page.map(entity -> NexusDailySettlementResponse.from(entity, false)));
    }

    @Transactional(readOnly = true)
    public NexusDailySettlementResponse settlement(String settlementId) {
        return settlementRepository.findBySettlementId(settlementId)
                .map(entity -> NexusDailySettlementResponse.from(entity, false))
                .orElseThrow(() -> new NotFoundException("Nexus daily settlement not found: " + settlementId));
    }

    @Transactional(readOnly = true)
    public NexusDailySettlementSummaryResponse summary(LocalDate date) {
        List<NexusDailySettlementEntity> rows = date == null
                ? settlementRepository.findAll()
                : settlementRepository.findBySettlementDate(date);
        return new NexusDailySettlementSummaryResponse(
                date,
                rows.size(),
                rows.stream().filter(row -> row.status() == NexusDailySettlementStatus.SENT).count(),
                rows.stream().filter(row -> row.status() == NexusDailySettlementStatus.DRY_RUN).count(),
                rows.stream().filter(row -> row.status() == NexusDailySettlementStatus.RETRY).count(),
                rows.stream().filter(row -> row.status() == NexusDailySettlementStatus.FAILED).count(),
                rows.stream().mapToLong(NexusDailySettlementEntity::totalShipments).sum(),
                rows.stream().mapToLong(NexusDailySettlementEntity::totalQuantity).sum(),
                rows.stream().mapToLong(NexusDailySettlementEntity::totalLogisticsCost).sum(),
                rows.stream().mapToLong(NexusDailySettlementEntity::manufacturingImpactCost).sum()
        );
    }

    private NexusDailySettlementResponse runFactory(LocalDate date, String factoryId) {
        LocalDateTime now = LocalDateTime.now(clock);
        NexusDailySettlementDraft draft = draft(date, factoryId);
        if (draft.totalShipments() == 0) {
            return new NexusDailySettlementResponse(
                    draft.settlementId(),
                    draft.idempotencyKey(),
                    draft.settlementDate(),
                    draft.factoryId(),
                    draft.currency(),
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    draft.manufacturingShareRate(),
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    "SKIPPED",
                    0,
                    "No route plans for settlement date and factory.",
                    null,
                    null,
                    false
            );
        }

        NexusDailySettlementEntity entity = settlementRepository
                .findBySettlementDateAndFactoryId(date, factoryId)
                .map(existing -> {
                    if (existing.status() != NexusDailySettlementStatus.SENT) {
                        existing.apply(draft, now);
                    }
                    return existing;
                })
                .orElseGet(() -> settlementRepository.save(new NexusDailySettlementEntity(draft, now)));

        if (entity.status() == NexusDailySettlementStatus.SENT) {
            auditLogService.record(AuditAction.NEXUS_DAILY_SETTLEMENT_SKIPPED, "nexus_daily_settlement",
                    entity.settlementId(), entity.status().name(), entity.status().name(),
                    Map.of("reason", "Already sent", "idempotencyKey", entity.idempotencyKey()));
            return NexusDailySettlementResponse.from(entity, true);
        }

        if (!properties.isEnabled()) {
            entity.markDryRun(now);
            auditLogService.record(AuditAction.NEXUS_DAILY_SETTLEMENT_DRY_RUN, "nexus_daily_settlement",
                    entity.settlementId(), null, entity.status().name(),
                    Map.of("endpoint", properties.endpoint(), "manufacturingImpactCost", entity.manufacturingImpactCost()));
            return NexusDailySettlementResponse.from(entity, false);
        }

        try {
            JsonNode response = nexusClient.publish(request(entity));
            entity.markSent(response, LocalDateTime.now(clock));
            auditLogService.record(AuditAction.NEXUS_DAILY_SETTLEMENT_SENT, "nexus_daily_settlement",
                    entity.settlementId(), null, entity.status().name(),
                    Map.of("endpoint", properties.endpoint(), "manufacturingImpactCost", entity.manufacturingImpactCost()));
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            entity.scheduleRetry(message, properties.getMaxRetryCount(), LocalDateTime.now(clock));
            auditLogService.record(AuditAction.NEXUS_DAILY_SETTLEMENT_FAILED, "nexus_daily_settlement",
                    entity.settlementId(), null, entity.status().name(),
                    Map.of("endpoint", properties.endpoint(), "error", message));
        }
        return NexusDailySettlementResponse.from(entity, false);
    }

    private NexusDailySettlementDraft draft(LocalDate date, String factoryId) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<RoutePlanEntity> plans = routePlanRepository.findByFactoryIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                factoryId, start, end);

        Map<String, LogisticsOutboxEntity> outboxByRoutePlanId = outboxRepository.findByAggregateIdIn(
                        plans.stream().map(RoutePlanEntity::routePlanId).toList()
                ).stream()
                .collect(Collectors.toMap(LogisticsOutboxEntity::aggregateId, Function.identity(), (first, ignored) -> first));
        List<RoutePlanEntity> settledPlans = plans.stream()
                .filter(plan -> {
                    LogisticsOutboxEntity outbox = outboxByRoutePlanId.get(plan.routePlanId());
                    return outbox != null && outbox.status() == OutboxStatus.PUBLISHED;
                })
                .toList();

        Map<String, RouteCostEntity> costByRoutePlanId = routeCostRepository.findByRoutePlanIdIn(
                        settledPlans.stream().map(RoutePlanEntity::routePlanId).toList()
                ).stream()
                .collect(Collectors.toMap(RouteCostEntity::routePlanId, Function.identity()));
        Map<String, NexusEventEntity> eventById = nexusEventRepository.findByEventIdIn(
                        settledPlans.stream().map(RoutePlanEntity::sourceEventId).toList()
                ).stream()
                .collect(Collectors.toMap(NexusEventEntity::eventId, Function.identity()));

        int totalShipments = settledPlans.size();
        int delayedShipments = (int) settledPlans.stream().filter(RoutePlanEntity::delayed).count();
        int heldShipments = (int) settledPlans.stream()
                .map(plan -> eventById.get(plan.sourceEventId()))
                .filter(event -> event != null && "SHIPMENT_HOLD_RELEASED".equals(event.eventType()))
                .count();
        long totalQuantity = settledPlans.stream()
                .map(plan -> eventById.get(plan.sourceEventId()))
                .filter(event -> event != null)
                .mapToLong(event -> event.payload().path("quantity").asLong(0))
                .sum();
        long totalLogisticsCost = costByRoutePlanId.values().stream().mapToLong(RouteCostEntity::totalCost).sum();
        BigDecimal shareRate = properties.getManufacturingShareRate().setScale(4, RoundingMode.HALF_UP);
        long manufacturingImpactCost = BigDecimal.valueOf(totalLogisticsCost)
                .multiply(shareRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        BigDecimal onTimeRate = totalShipments == 0
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(totalShipments - delayedShipments)
                .divide(BigDecimal.valueOf(totalShipments), 4, RoundingMode.HALF_UP);
        return new NexusDailySettlementDraft(
                settlementId(date, factoryId),
                "LOGISTICS:DAILY:" + date + ":" + factoryId,
                SOURCE,
                date,
                factoryId,
                CURRENCY,
                totalShipments,
                delayedShipments,
                heldShipments,
                totalQuantity,
                totalLogisticsCost,
                manufacturingImpactCost,
                shareRate,
                onTimeRate
        );
    }

    private NexusDailySettlementRequest request(NexusDailySettlementEntity entity) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("basis", "published synthetic daily route cost summary");
        evidence.put("settlementBasis", "logistics_outbox_event.status=PUBLISHED");
        evidence.put("manufacturingShareRate", entity.manufacturingShareRate());
        evidence.put("source", "Archive-Logistics route_plan + route_cost");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("demoData", true);
        payload.put("syntheticData", true);
        payload.put("settlementRole", "Manufacturing compensation callback from Logistics to Nexus");
        payload.put("ledgerIntegration", "Archive-Ledger receives logistics cost events separately through outbox.");

        return new NexusDailySettlementRequest(
                entity.settlementId(),
                entity.idempotencyKey(),
                entity.source(),
                1,
                entity.settlementDate(),
                entity.factoryId(),
                entity.currency(),
                entity.totalShipments(),
                entity.delayedShipments(),
                entity.heldShipments(),
                entity.totalQuantity(),
                entity.totalLogisticsCost(),
                entity.manufacturingImpactCost(),
                entity.onTimeRate(),
                evidence,
                payload,
                Instant.now(clock)
        );
    }

    private int count(List<NexusDailySettlementResponse> results, NexusDailySettlementStatus status) {
        return (int) results.stream().filter(result -> status.name().equals(result.status())).count();
    }

    private String settlementId(LocalDate date, String factoryId) {
        return "LGS-SETTLE-" + date.format(BASIC_DATE) + "-" + factoryId;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
