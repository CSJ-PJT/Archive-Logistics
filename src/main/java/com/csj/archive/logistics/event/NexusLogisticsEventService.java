package com.csj.archive.logistics.event;

import com.csj.archive.logistics.audit.AuditAction;
import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.common.BusinessException;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.economy.LogisticsEconomyService;
import com.csj.archive.logistics.ledger.LedgerContractMode;
import com.csj.archive.logistics.ledger.LedgerPublishProperties;
import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.csj.archive.logistics.outbox.LogisticsOutboxEvent;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.route.RouteCostEntity;
import com.csj.archive.logistics.route.RouteCostRepository;
import com.csj.archive.logistics.route.RoutePlan;
import com.csj.archive.logistics.route.RoutePlanEntity;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.csj.archive.logistics.route.SyntheticRouteCalculator;
import com.csj.archive.logistics.workforce.WorkforceService;
import com.csj.archive.logistics.workforce.WorkforceSummaryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NexusLogisticsEventService {
    private static final String SOURCE = "Archive-Logitics";

    private final NexusEventRepository nexusEventRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteCostRepository routeCostRepository;
    private final LogisticsOutboxRepository outboxRepository;
    private final SyntheticRouteCalculator routeCalculator;
    private final LedgerPublishProperties ledgerProperties;
    private final LogisticsEconomyService economyService;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final IdGenerator idGenerator;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final WorkforceService workforceService;

    public NexusLogisticsEventService(NexusEventRepository nexusEventRepository,
                                      RoutePlanRepository routePlanRepository,
                                      RouteCostRepository routeCostRepository,
                                      LogisticsOutboxRepository outboxRepository,
                                      SyntheticRouteCalculator routeCalculator,
                                      LedgerPublishProperties ledgerProperties,
                                      LogisticsEconomyService economyService,
                                      ObjectMapper objectMapper,
                                      AuditLogService auditLogService,
                                      IdGenerator idGenerator,
                                      TransactionTemplate transactionTemplate,
                                      MeterRegistry meterRegistry,
                                      Clock clock,
                                      WorkforceService workforceService) {
        this.nexusEventRepository = nexusEventRepository;
        this.routePlanRepository = routePlanRepository;
        this.routeCostRepository = routeCostRepository;
        this.outboxRepository = outboxRepository;
        this.routeCalculator = routeCalculator;
        this.ledgerProperties = ledgerProperties;
        this.economyService = economyService;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.idGenerator = idGenerator;
        this.transactionTemplate = transactionTemplate;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        this.workforceService = workforceService;
    }

    public EventProcessingResult process(NexusLogisticsEventRequest request) {
        return transactionTemplate.execute(status -> processInTransaction(request));
    }

    public BulkEventProcessingResult processBulk(NexusLogisticsBulkEventRequest request) {
        List<NexusLogisticsEventRequest> events = request.events();
        int[] counts = new int[7];
        int chunkSize = 50;
        for (int offset = 0; offset < events.size(); offset += chunkSize) {
            int from = offset;
            int to = Math.min(offset + chunkSize, events.size());
            transactionTemplate.executeWithoutResult(status -> {
                List<NexusLogisticsEventRequest> chunk = events.subList(from, to);
                Map<String, NexusEventEntity> duplicateByEventId = nexusEventRepository.findByEventIdIn(
                                chunk.stream().map(NexusLogisticsEventRequest::eventId).toList()
                        ).stream()
                        .collect(Collectors.toMap(NexusEventEntity::eventId, Function.identity()));
                Map<String, NexusEventEntity> duplicateByIdempotencyKey = nexusEventRepository.findByIdempotencyKeyIn(
                                chunk.stream().map(NexusLogisticsEventRequest::idempotencyKey).toList()
                        ).stream()
                        .collect(Collectors.toMap(NexusEventEntity::idempotencyKey, Function.identity()));
                for (NexusLogisticsEventRequest event : chunk) {
                    try {
                        NexusEventEntity duplicate = duplicateByEventId.get(event.eventId());
                        if (duplicate == null) {
                            duplicate = duplicateByIdempotencyKey.get(event.idempotencyKey());
                        }
                        accumulate(counts, duplicate == null ? processNewInTransaction(event) : duplicateResult(duplicate, event));
                    } catch (RuntimeException exception) {
                        counts[2]++;
                    }
                }
            });
        }
        return new BulkEventProcessingResult(events.size(), counts[0], counts[1], counts[2], counts[3],
                counts[4], counts[5], counts[6]);
    }

    private void accumulate(int[] counts, EventProcessingResult result) {
        if (result.duplicate()) {
            counts[1]++;
        } else if (result.failed()) {
            counts[2]++;
        } else {
            counts[0]++;
        }
        if (result.outboxEventId() != null) {
            counts[3]++;
        }
        if (result.requiresApproval()) {
            counts[4]++;
        }
        if (result.delayed()) {
            counts[5]++;
        }
        if (result.deviated()) {
            counts[6]++;
        }
    }

    private EventProcessingResult processInTransaction(NexusLogisticsEventRequest request) {
        Optional<NexusEventEntity> duplicate = nexusEventRepository.findByEventId(request.eventId())
                .or(() -> nexusEventRepository.findByIdempotencyKey(request.idempotencyKey()));
        if (duplicate.isPresent()) {
            return duplicateResult(duplicate.get(), request);
        }
        return processNewInTransaction(request);
    }

    private EventProcessingResult processNewInTransaction(NexusLogisticsEventRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
            NexusEventEntity event = nexusEventRepository.save(new NexusEventEntity(
                    request.eventId(),
                    request.idempotencyKey(),
                    request.source(),
                    request.eventType(),
                    objectMapper.valueToTree(request.payload()),
                    now
            ));
            auditLogService.record(AuditAction.NEXUS_EVENT_RECEIVED, "nexus_logistics_event", event.eventId(),
                    null, NexusEventStatus.RECEIVED.name(), Map.of("eventType", request.eventType()));

        try {
            MarketShipmentMetadata metadata = MarketShipmentMetadata.fromPayload(request.payload());
            RoutePlan routePlan = routeCalculator.calculate(request, metadata);
            routePlanRepository.save(new RoutePlanEntity(routePlan, now));
            RouteCostEntity routeCost = routeCostRepository.save(new RouteCostEntity(routePlan.routePlanId(), routePlan.cost(), now));
            var economy = economyService.createRouteEconomyEvents(
                    routePlan,
                    routeCost,
                    request.eventId(),
                    request.eventId(),
                    metadata.simulationRunId(),
                    metadata.settlementCycleId()
            );

            String ledgerEventType = ledgerProperties.getContractMode() == LedgerContractMode.ARCHIVE_LEDGER_V1_COMPAT
                    ? "LOGISTICS_DISPATCHED"
                    : ledgerEventType(routePlan, request.eventType());
            String outboxEventId = idGenerator.logiticsEventId(ledgerEventType, routePlan.routePlanId(), request.occurredAt());
            String outboxIdempotencyKey = "LOGISTICS:" + ledgerEventType + ":" + routePlan.routePlanId();
            LogisticsOutboxEntity outbox = outboxRepository.save(new LogisticsOutboxEntity(
                    new LogisticsOutboxEvent(
                            outboxEventId,
                            outboxIdempotencyKey,
                            SOURCE,
                            ledgerEventType,
                            "ROUTE_PLAN",
                            routePlan.routePlanId(),
                            ledgerPayload(routePlan, routeCost, economy, request.eventId(), request.eventId(), metadata)
                    ),
                    now
            ));
            event.markProcessed(now);
            meterRegistry.counter("archive.logitics.events.processed").increment();
            auditLogService.record(AuditAction.ROUTE_PLAN_CREATED, "route_plan", routePlan.routePlanId(),
                    null, routePlan.routeStatus(), Map.of("sourceEventId", request.eventId()));
            auditLogService.record(AuditAction.OUTBOX_EVENT_CREATED, "logistics_outbox_event", outbox.eventId(),
                    null, outbox.status().name(), Map.of("routePlanId", routePlan.routePlanId()));
            auditLogService.record(AuditAction.NEXUS_EVENT_PROCESSED, "nexus_logistics_event", event.eventId(),
                    NexusEventStatus.RECEIVED.name(), NexusEventStatus.PROCESSED.name(),
                    Map.of("routePlanId", routePlan.routePlanId(), "outboxEventId", outbox.eventId()));

            return new EventProcessingResult(
                    event.eventId(),
                    event.idempotencyKey(),
                    event.status().name(),
                    false,
                    routePlan.routePlanId(),
                    outbox.eventId(),
                    routeCost.requiresApproval(),
                    routePlan.delayed(),
                    routePlan.deviated(),
                    routeCost.totalCost(),
                    null
            );
        } catch (BusinessException exception) {
            event.markFailed(exception.getMessage(), now);
            meterRegistry.counter("archive.logitics.events.failed").increment();
            auditLogService.record(AuditAction.NEXUS_EVENT_FAILED, "nexus_logistics_event", event.eventId(),
                    NexusEventStatus.RECEIVED.name(), NexusEventStatus.FAILED.name(),
                    Map.of("error", exception.getMessage()));
            return new EventProcessingResult(event.eventId(), event.idempotencyKey(), event.status().name(),
                    false, null, null, false, false, false, null, exception.getMessage());
        }
    }

    private EventProcessingResult duplicateResult(NexusEventEntity event, NexusLogisticsEventRequest incoming) {
        meterRegistry.counter("archive.logitics.events.duplicate").increment();
        auditLogService.record(AuditAction.DUPLICATE_EVENT_RECEIVED, "nexus_logistics_event",
                event.eventId(), event.status().name(), event.status().name(),
                Map.of("incomingEventId", incoming.eventId(), "incomingIdempotencyKey", incoming.idempotencyKey()));
        return duplicateResult(event);
    }

    private EventProcessingResult duplicateResult(NexusEventEntity event) {
        RoutePlanEntity routePlan = routePlanRepository.findBySourceEventId(event.eventId()).orElse(null);
        RouteCostEntity routeCost = routePlan == null ? null : routeCostRepository.findByRoutePlanId(routePlan.routePlanId()).orElse(null);
        LogisticsOutboxEntity outbox = routePlan == null ? null : outboxRepository.findByAggregateId(routePlan.routePlanId()).orElse(null);
        return new EventProcessingResult(
                event.eventId(),
                event.idempotencyKey(),
                event.status().name(),
                true,
                routePlan == null ? null : routePlan.routePlanId(),
                outbox == null ? null : outbox.eventId(),
                routeCost != null && routeCost.requiresApproval(),
                routePlan != null && routePlan.delayed(),
                routePlan != null && routePlan.deviated(),
                routeCost == null ? null : routeCost.totalCost(),
                event.failureReason()
        );
    }

    private JsonNode ledgerPayload(RoutePlan routePlan, RouteCostEntity routeCost, LogisticsEconomyService.RouteEconomyResult economy,
                                   String correlationId, String causationId, MarketShipmentMetadata metadata) {
        String safeCorrelationId = metadata == null || metadata.correlationId() == null ? correlationId : metadata.correlationId();
        String safeCausationId = metadata == null || metadata.causationId() == null ? causationId : metadata.causationId();
        String sourceChain = "Archive-Market -> Archive-Nexus -> Archive-Logitics";
        WorkforceSummaryResponse workforce = workforceService.workforceSummary();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceService", "Archive-Logistics");
        payload.put("billedToService", "Archive-Nexus");
        payload.put("logisticsRevenue", economy.totalRevenue());
        payload.put("logisticsCost", economy.totalCost());
        payload.put("logisticsProfit", economy.netProfit());
        payload.put("sourceChain", sourceChain);
        payload.put("orderId", routePlan.orderId());
        payload.put("customerId", routePlan.customerId());
        payload.put("customerType", routePlan.customerType());
        payload.put("productType", routePlan.productType());
        payload.put("orderAmount", routePlan.orderAmount());
        payload.put("totalAmount", routePlan.totalAmount());
        payload.put("marketPriority", routePlan.marketPriority());
        payload.put("riskLevel", routePlan.riskLevel());
        payload.put("expressOrder", routePlan.expressOrder());
        payload.put("workdayId", workforce.workdayId());
        payload.put("workforceAllocationId", workforce.allocationId());
        payload.put("productivityScore", workforce.capacityEvents() <= 0 ? 0 : (double) workforce.processedEvents() / workforce.capacityEvents());
        payload.put("driverCapacityUsed", workforce.deliveryCompleted());
        payload.put("backlogCount", workforce.backlogEvents());
        payload.put("delayedCount", workforce.shipmentsDelayed());
        payload.put("bottleneckRole", workforce.bottleneckType());
        payload.put("payrollCost", workforce.syntheticLaborCost());
        payload.put("factoryId", routePlan.factoryId());
        payload.put("shipmentId", routePlan.shipmentId());
        payload.put("vendorId", routePlan.vendorId());
        payload.put("severity", severity(routePlan));
        payload.put("estimatedCost", routePlan.cost().totalCost());
        payload.put("totalCost", routePlan.cost().totalCost());
        payload.put("currency", routePlan.cost().currency());
        payload.put("reason", routePlan.cost().reason());
        payload.put("requiresApproval", routePlan.cost().requiresApproval());
        payload.put("routePlanId", routePlan.routePlanId());
        payload.put("originCode", routePlan.originCode());
        payload.put("destinationCode", routePlan.destinationCode());
        payload.put("distanceKm", routePlan.distanceKm());
        payload.put("estimatedMinutes", routePlan.estimatedMinutes());
        payload.put("fuelCost", routePlan.cost().fuelCost());
        payload.put("tollCost", routePlan.cost().tollCost());
        payload.put("urgentSurcharge", routePlan.cost().urgentSurcharge());
        payload.put("delayPenalty", routePlan.cost().delayPenalty());
        payload.put("coldChainPenalty", routePlan.cost().coldChainPenalty());
        payload.put("riskScore", routePlan.riskScore());
        payload.put("delayed", routePlan.delayed());
        payload.put("deviated", routePlan.deviated());
        payload.put("priority", routePlan.priority());
        payload.put("requiresColdChain", routePlan.requiresColdChain());
        payload.put("simulationRunId", routePlan.simulationRunId());
        payload.put("settlementCycleId", routePlan.settlementCycleId());
        payload.put("correlationId", safeCorrelationId);
        payload.put("causationId", safeCausationId);
        int maxHop = routePlan.maxHop() == null ? economyService.maxHop() : Math.max(1, routePlan.maxHop());
        int hopCount = (routePlan.hopCount() == null ? 0 : routePlan.hopCount()) + 1;
        payload.put("hopCount", hopCount);
        payload.put("maxHop", maxHop);
        payload.put("routeStatus", routePlan.routeStatus());
        payload.put("source", SOURCE);
        return objectMapper.valueToTree(payload);
    }

    private String ledgerEventType(RoutePlan routePlan, String sourceEventType) {
        if (routePlan.requiresColdChain() && routePlan.delayed()) {
            return "COLD_CHAIN_RISK_COST_CONFIRMED";
        }
        if (routePlan.deviated()) {
            return "ROUTE_DEVIATION_COST_CONFIRMED";
        }
        if (routePlan.delayed()) {
            return "DELAY_PENALTY_CONFIRMED";
        }
        if ("URGENT_DELIVERY_REQUESTED".equals(sourceEventType) || routePlan.cost().urgentSurcharge() > 0) {
            return "URGENT_DELIVERY_COST_CONFIRMED";
        }
        return "LOGISTICS_COST_CONFIRMED";
    }

    private String severity(RoutePlan routePlan) {
        if (!routePlan.cost().requiresApproval()) {
            return "NORMAL";
        }
        return "CRITICAL".equals(routePlan.priority()) || routePlan.deviated() ? "CRITICAL" : "HIGH";
    }
}
