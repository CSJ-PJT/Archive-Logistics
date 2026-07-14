package com.csj.archive.logistics.runtime;

import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.event.NexusLogisticsEventRequest;
import com.csj.archive.logistics.route.RoutePlanEntity;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.csj.archive.logistics.workforce.WorkdayProductivityResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShipmentLifecycleService {
    private static final String SERVICE = "Archive-Logistics";
    private static final java.util.Set<String> ROUTE_OUTBOX_PROJECTIONS = java.util.Set.of(
            "SHIPMENT_CREATED", "ROUTE_ASSIGNED", "ROUTE_COST_CALCULATED", "LOGISTICS_COST_CONFIRMED");

    private final RoutePlanRepository routePlanRepository;
    private final ShipmentRuntimeEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;
    private final ArchiveOsRuntimeDeliveryService archiveOsRuntimeDeliveryService;
    private final Clock clock;

    @Autowired
    public ShipmentLifecycleService(RoutePlanRepository routePlanRepository,
                                    ShipmentRuntimeEventRepository eventRepository,
                                    ObjectMapper objectMapper,
                                    IdGenerator idGenerator, ArchiveOsRuntimeDeliveryService archiveOsRuntimeDeliveryService,
                                    Clock clock) {
        this.routePlanRepository = routePlanRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.archiveOsRuntimeDeliveryService = archiveOsRuntimeDeliveryService;
        this.clock = clock;
    }

    ShipmentLifecycleService(RoutePlanRepository routePlanRepository,
                             ShipmentRuntimeEventRepository eventRepository,
                             ObjectMapper objectMapper,
                             IdGenerator idGenerator,
                             Clock clock) {
        this(routePlanRepository, eventRepository, objectMapper, idGenerator, null, clock);
    }

    @Transactional
    public ShipmentLifecycleResult advance(List<NexusLogisticsEventRequest> requests,
                                            WorkdayProductivityResult workday) {
        if (requests == null || requests.isEmpty()) {
            return ShipmentLifecycleResult.empty();
        }
        Map<String, NexusLogisticsEventRequest> byEventId = requests.stream()
                .collect(java.util.stream.Collectors.toMap(NexusLogisticsEventRequest::eventId, request -> request, (left, right) -> left));
        List<RoutePlanEntity> routes = routePlanRepository.findBySourceEventIdIn(byEventId.keySet()).stream()
                .sorted(Comparator.comparingInt(this::priorityRank).reversed())
                .toList();

        int completionBudget = workday == null ? 0 : (int) Math.min(routes.size(), Math.max(0L, workday.deliveryCompleted()));
        int completed = 0;
        int delayed = 0;
        for (RoutePlanEntity route : routes) {
            NexusLogisticsEventRequest request = byEventId.get(route.sourceEventId());
            if (isTerminal(route.routeStatus())) {
                continue;
            }
            initialize(route, request, workday);
            if (hopExceeded(route)) {
                delayed += delay(route, workday, "Hop guard blocked further shipment progression.");
                continue;
            }
            if (route.delayed() || completed >= completionBudget) {
                delayed += delay(route, workday, route.delayed()
                        ? "Synthetic route risk requires delayed delivery handling."
                        : "Synthetic delivery capacity is exhausted for this runtime tick.");
                continue;
            }
            LocalDateTime now = LocalDateTime.now(clock);
            route.transitionTo("DELIVERY_IN_TRANSIT", now);
            record(route, "TRUCK_DISPATCHED", "PROCESSING", "INFO", workday, "Synthetic shipment dispatched.");
            record(route, "DELIVERY_IN_TRANSIT", "PROCESSING", "INFO", workday, "Synthetic shipment is in transit.");
            route.transitionTo("DELIVERY_COMPLETED", now);
            record(route, "DELIVERY_COMPLETED", "COMPLETED", "NORMAL", workday, "Synthetic delivery completed.");
            completed++;
        }
        return new ShipmentLifecycleResult(routes.size(), completed, delayed);
    }

    private void initialize(RoutePlanEntity route, NexusLogisticsEventRequest request, WorkdayProductivityResult workday) {
        record(route, "SHIPMENT_CREATED", "CREATED", "INFO", workday, "Nexus shipment request received.");
        record(route, "ROUTE_ASSIGNED", "PROCESSING", "INFO", workday, "Synthetic route and ETA assigned.");
        record(route, "ROUTE_COST_CALCULATED", "COMPLETED", route.riskScore().doubleValue() >= 0.85 ? "WARNING" : "INFO", workday,
                "Synthetic ETA, cost, and risk calculated.");
        record(route, "LOGISTICS_COST_CONFIRMED", "PROCESSING", "INFO", workday,
                "Ledger-compatible logistics cost event created.");
    }

    private int delay(RoutePlanEntity route, WorkdayProductivityResult workday, String reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        route.transitionTo("DELIVERY_DELAYED", now);
        record(route, "DELIVERY_DELAYED", "DELAYED", "WARNING", workday, reason);
        if (route.requiresColdChain()) {
            record(route, "COLD_CHAIN_RISK_DETECTED", "DELAYED", "WARNING", workday,
                    "Synthetic cold-chain handling risk detected during delayed delivery.");
        }
        return 1;
    }

    private void record(RoutePlanEntity route, String eventType, String status, String severity,
                        WorkdayProductivityResult workday, String label) {
        if (eventRepository.existsByRoutePlanIdAndEventType(route.routePlanId(), eventType)) {
            return;
        }
        String idempotencyKey = "LOGISTICS:RUNTIME:" + eventType + ":" + route.routePlanId();
        String eventId = idGenerator.logiticsEventId(eventType, route.routePlanId());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("routePlanId", route.routePlanId());
        metadata.put("shipmentId", route.shipmentId());
        metadata.put("orderId", route.orderId());
        metadata.put("customerType", route.customerType());
        metadata.put("productType", route.productType());
        metadata.put("priority", route.priority());
        metadata.put("destinationType", destinationType(route.destinationCode()));
        metadata.put("syntheticHubId", route.destinationCode());
        metadata.put("estimatedMinutes", route.estimatedMinutes());
        metadata.put("riskScore", route.riskScore());
        if (workday != null) {
            metadata.put("workdayId", workday.workdayId());
            metadata.put("backlogCount", workday.backlogEvents());
            metadata.put("bottleneckRole", workday.bottleneckType());
        }
        ShipmentRuntimeEventEntity saved = eventRepository.save(new ShipmentRuntimeEventEntity(
                eventId, idempotencyKey, route.routePlanId(), route.shipmentId(), SERVICE,
                "ArchiveOS", eventType, status, severity,
                fallback(route.correlationId(), route.sourceEventId()),
                route.causationId(), route.simulationRunId(), route.settlementCycleId(),
                workday == null ? null : workday.workdayId(), safe(route.hopCount()), safeMax(route.maxHop()),
                objectMapper.valueToTree(metadata), LocalDateTime.now(clock)
        ));
        if (archiveOsRuntimeDeliveryService != null && !ROUTE_OUTBOX_PROJECTIONS.contains(eventType)) {
            try { archiveOsRuntimeDeliveryService.snapshot(saved); } catch (RuntimeException ignored) { /* observability must not affect shipment progression */ }
        }
    }

    private boolean isTerminal(String status) {
        return "DELIVERY_COMPLETED".equals(status) || "DELIVERY_DELAYED".equals(status);
    }

    private boolean hopExceeded(RoutePlanEntity route) {
        return safe(route.hopCount()) > safeMax(route.maxHop());
    }

    private int priorityRank(RoutePlanEntity route) {
        return switch (route.priority()) {
            case "CRITICAL" -> 3;
            case "HIGH" -> 2;
            default -> 1;
        };
    }

    private String destinationType(String code) {
        return code != null && code.startsWith("DC-") ? "DISTRIBUTION_CENTER" : "SYNTHETIC_HUB";
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int safe(Integer value) { return value == null ? 0 : Math.max(0, value); }
    private int safeMax(Integer value) { return value == null || value <= 0 ? 5 : value; }

    public record ShipmentLifecycleResult(int requested, int completed, int delayed) {
        static ShipmentLifecycleResult empty() { return new ShipmentLifecycleResult(0, 0, 0); }
    }
}
