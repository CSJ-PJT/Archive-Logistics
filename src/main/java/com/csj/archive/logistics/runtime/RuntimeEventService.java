package com.csj.archive.logistics.runtime;

import com.csj.archive.logistics.event.NexusEventEntity;
import com.csj.archive.logistics.event.NexusEventRepository;
import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.outbox.OutboxStatus;
import com.csj.archive.logistics.route.RouteCostEntity;
import com.csj.archive.logistics.route.RouteCostRepository;
import com.csj.archive.logistics.route.RoutePlanEntity;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.csj.archive.logistics.workforce.WorkdayProductivityEntity;
import com.csj.archive.logistics.workforce.WorkdayProductivityRepository;
import com.csj.archive.logistics.workforce.WorkforceAllocationEntity;
import com.csj.archive.logistics.workforce.WorkforceAllocationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RuntimeEventService {
    private static final String SERVICE = "Archive-Logistics";

    private final NexusEventRepository nexusEventRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteCostRepository routeCostRepository;
    private final LogisticsOutboxRepository outboxRepository;
    private final WorkforceAllocationRepository workforceAllocationRepository;
    private final WorkdayProductivityRepository workdayProductivityRepository;
    private final ShipmentRuntimeEventRepository shipmentRuntimeEventRepository;
    private final ObjectMapper objectMapper;

    public RuntimeEventService(NexusEventRepository nexusEventRepository,
                               RoutePlanRepository routePlanRepository,
                               RouteCostRepository routeCostRepository,
                               LogisticsOutboxRepository outboxRepository,
                               WorkforceAllocationRepository workforceAllocationRepository,
                               WorkdayProductivityRepository workdayProductivityRepository,
                               ShipmentRuntimeEventRepository shipmentRuntimeEventRepository,
                               ObjectMapper objectMapper) {
        this.nexusEventRepository = nexusEventRepository;
        this.routePlanRepository = routePlanRepository;
        this.routeCostRepository = routeCostRepository;
        this.outboxRepository = outboxRepository;
        this.workforceAllocationRepository = workforceAllocationRepository;
        this.workdayProductivityRepository = workdayProductivityRepository;
        this.shipmentRuntimeEventRepository = shipmentRuntimeEventRepository;
        this.objectMapper = objectMapper;
    }

    public List<RuntimeEventResponse> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return allRecent(safeLimit).stream()
                .sorted(Comparator.comparing(RuntimeEventResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(RuntimeEventResponse::eventId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
    }

    public List<RuntimeEventResponse> recentAfter(String after, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return RuntimeEventCursor.decode(after)
                .map(cursor -> allRecent(500).stream()
                        .filter(event -> isAfter(event, cursor))
                        .sorted(Comparator.comparing(RuntimeEventResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(RuntimeEventResponse::eventId, Comparator.nullsLast(Comparator.naturalOrder())))
                        .limit(safeLimit)
                        .toList())
                .orElseGet(List::of);
    }

    public String latestCursor() {
        return allRecent(500).stream()
                .sorted(Comparator.comparing(RuntimeEventResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(RuntimeEventResponse::eventId, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .map(RuntimeEventResponse::cursor)
                .orElse(null);
    }

    private List<RuntimeEventResponse> allRecent(int sourceLimit) {
        PageRequest page = PageRequest.of(0, sourceLimit);
        List<RuntimeEventResponse> events = new ArrayList<>();
        nexusEventRepository.findAllByOrderByCreatedAtDesc(page).forEach(event -> events.add(fromNexus(event)));
        routePlanRepository.findAllByOrderByCreatedAtDesc(page).forEach(route -> events.addAll(fromRoute(route)));
        routeCostRepository.findAllByOrderByCreatedAtDesc(page).forEach(cost -> events.add(fromRouteCost(cost)));
        outboxRepository.findAllByOrderByCreatedAtDesc(page).forEach(outbox -> events.addAll(fromOutbox(outbox)));
        workforceAllocationRepository.findAllByOrderByCreatedAtDesc(page).forEach(allocation -> events.add(fromAllocation(allocation)));
        workdayProductivityRepository.findAllByOrderByCreatedAtDesc(page).forEach(workday -> events.addAll(fromWorkday(workday)));
        shipmentRuntimeEventRepository.findAllByOrderByOccurredAtDesc(page).forEach(event -> events.add(fromShipmentRuntime(event)));
        return events;
    }

    private boolean isAfter(RuntimeEventResponse event, RuntimeEventCursor.Position cursor) {
        if (event.occurredAt() == null || event.eventId() == null) {
            return false;
        }
        int timeComparison = event.occurredAt().compareTo(cursor.occurredAt());
        return timeComparison > 0 || (timeComparison == 0 && event.eventId().compareTo(cursor.eventId()) > 0);
    }

    public List<RuntimeEventResponse> byCorrelation(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return List.of();
        }
        List<RuntimeEventResponse> events = new ArrayList<>();
        routePlanRepository.findByCorrelationId(correlationId).forEach(route -> {
            events.addAll(fromRoute(route));
            routeCostRepository.findByRoutePlanId(route.routePlanId()).ifPresent(cost -> events.add(fromRouteCost(cost)));
            outboxRepository.findByAggregateId(route.routePlanId()).ifPresent(outbox -> events.addAll(fromOutbox(outbox)));
            nexusEventRepository.findByEventId(route.sourceEventId()).ifPresent(event -> events.add(fromNexus(event)));
        });
        shipmentRuntimeEventRepository.findByCorrelationIdOrderByOccurredAtDesc(correlationId).forEach(event -> events.add(fromShipmentRuntime(event)));
        return sorted(events);
    }

    public List<RuntimeEventResponse> byEntity(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return List.of();
        }
        List<RuntimeEventResponse> events = new ArrayList<>();
        nexusEventRepository.findByEventId(entityId).ifPresent(event -> events.add(fromNexus(event)));
        routePlanRepository.findByRoutePlanId(entityId).ifPresent(route -> {
            events.addAll(fromRoute(route));
            routeCostRepository.findByRoutePlanId(route.routePlanId()).ifPresent(cost -> events.add(fromRouteCost(cost)));
            outboxRepository.findByAggregateId(route.routePlanId()).ifPresent(outbox -> events.addAll(fromOutbox(outbox)));
        });
        routePlanRepository.findByShipmentId(entityId).forEach(route -> events.addAll(fromRoute(route)));
        shipmentRuntimeEventRepository.findByShipmentIdOrderByOccurredAtDesc(entityId).forEach(event -> events.add(fromShipmentRuntime(event)));
        outboxRepository.findByEventId(entityId).ifPresent(outbox -> events.addAll(fromOutbox(outbox)));
        outboxRepository.findByAggregateId(entityId).ifPresent(outbox -> events.addAll(fromOutbox(outbox)));
        return sorted(events);
    }

    private List<RuntimeEventResponse> sorted(List<RuntimeEventResponse> events) {
        return events.stream()
                .sorted(Comparator.comparing(RuntimeEventResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(RuntimeEventResponse::eventId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private RuntimeEventResponse fromNexus(NexusEventEntity event) {
        JsonNode payload = event.payload();
        String shipmentId = text(payload, "shipmentId", event.eventId());
        String correlationId = text(payload, "correlationId", event.eventId());
        String causationId = text(payload, "causationId", event.eventId());
        return new RuntimeEventResponse(
                event.eventId(),
                event.source(),
                "logistics",
                "SHIPMENT_CREATED",
                "nexus_logistics_event",
                shipmentId,
                correlationId,
                causationId,
                switchStatus(event.status().name()),
                event.status().name().equals("FAILED") ? "critical" : "info",
                "Nexus logistics event received",
                event.receivedAt(),
                metadata(
                        "shipmentId", shipmentId,
                        "originType", syntheticLocationType(text(payload, "originCode", null)),
                        "destinationType", syntheticLocationType(text(payload, "destinationCode", null)),
                        "priority", text(payload, "priority", null),
                        "orderId", text(payload, "orderId", null),
                        "idempotencyKey", event.idempotencyKey(),
                        "targetService", SERVICE,
                        "simulationRunId", text(payload, "simulationRunId", null),
                        "settlementCycleId", text(payload, "settlementCycleId", null),
                        "workdayId", text(payload, "workdayId", null),
                        "hopCount", text(payload, "hopCount", "0"),
                        "maxHop", text(payload, "maxHop", "5")
                )
        );
    }

    private List<RuntimeEventResponse> fromRoute(RoutePlanEntity route) {
        RuntimeEventResponse assigned = new RuntimeEventResponse(
                route.routePlanId(),
                SERVICE,
                "logistics",
                "ROUTE_ASSIGNED",
                "route_plan",
                route.routePlanId(),
                fallback(route.correlationId(), route.sourceEventId()),
                fallback(route.causationId(), route.sourceEventId()),
                "completed",
                "normal",
                "Synthetic route and ETA calculated",
                route.createdAt(),
                metadata(
                        "shipmentId", route.shipmentId(),
                        "factoryId", route.factoryId(),
                        "originType", syntheticLocationType(route.originCode()),
                        "destinationType", syntheticLocationType(route.destinationCode()),
                        "orderId", route.orderId(),
                        "targetService", "Archive-Ledger",
                        "idempotencyKey", "RUNTIME:ROUTE_ASSIGNED:" + route.routePlanId(),
                        "simulationRunId", route.simulationRunId(),
                        "settlementCycleId", route.settlementCycleId(),
                        "hopCount", 0,
                        "maxHop", 5
                )
        );
        return List.of(assigned);
    }

    private RuntimeEventResponse fromShipmentRuntime(ShipmentRuntimeEventEntity event) {
        return new RuntimeEventResponse(
                event.eventId(), event.idempotencyKey(), event.sourceService(), event.targetService(), "logistics",
                event.eventType(), "shipment", event.shipmentId(), event.correlationId(), event.causationId(),
                event.simulationRunId(), event.settlementCycleId(), event.workdayId(), event.status(), event.severity(),
                event.eventType().replace('_', ' '), event.occurredAt(), event.hopCount(), event.maxHop(),
                event.metadata() == null ? Map.of() : objectMetadata(event.metadata()),
                RuntimeEventCursor.encode(event.occurredAt(), event.eventId())
        );
    }

    private Map<String, Object> objectMetadata(JsonNode metadata) {
        return objectMapper.convertValue(metadata, new TypeReference<Map<String, Object>>() { });
    }

    private RuntimeEventResponse routeLifecycle(RoutePlanEntity route, String eventType, String status, String severity, String label) {
        return new RuntimeEventResponse(
                route.routePlanId() + ":" + eventType,
                SERVICE,
                "logistics",
                eventType,
                "shipment",
                route.shipmentId(),
                fallback(route.correlationId(), route.sourceEventId()),
                fallback(route.causationId(), route.sourceEventId()),
                status,
                severity,
                label,
                route.createdAt(),
                metadata(
                        "routePlanId", route.routePlanId(),
                        "shipmentId", route.shipmentId(),
                        "orderId", route.orderId(),
                        "destinationType", syntheticLocationType(route.destinationCode()),
                        "delayed", route.delayed(),
                        "targetService", "Archive-Ledger",
                        "idempotencyKey", "RUNTIME:" + eventType + ":" + route.routePlanId(),
                        "simulationRunId", route.simulationRunId(),
                        "settlementCycleId", route.settlementCycleId(),
                        "hopCount", 0,
                        "maxHop", 5
                )
        );
    }

    private RuntimeEventResponse fromRouteCost(RouteCostEntity cost) {
        return new RuntimeEventResponse(
                cost.routePlanId() + ":ROUTE_COST_CALCULATED",
                SERVICE,
                "logistics",
                "ROUTE_COST_CALCULATED",
                "route_cost",
                cost.routePlanId(),
                fallback(cost.correlationId(), cost.routePlanId()),
                fallback(cost.correlationId(), cost.routePlanId()),
                "completed",
                cost.requiresApproval() ? "warning" : "info",
                "Synthetic logistics route cost calculated",
                cost.createdAt(),
                metadata(
                        "routePlanId", cost.routePlanId(),
                        "orderId", cost.orderId(),
                        "totalCost", cost.totalCost(),
                        "currency", cost.currency(),
                        "requiresApproval", cost.requiresApproval(),
                        "targetService", "Archive-Ledger",
                        "idempotencyKey", "RUNTIME:ROUTE_COST_CALCULATED:" + cost.routePlanId(),
                        "simulationRunId", cost.simulationRunId(),
                        "settlementCycleId", cost.settlementCycleId(),
                        "hopCount", 0,
                        "maxHop", 5
                )
        );
    }

    private List<RuntimeEventResponse> fromOutbox(LogisticsOutboxEntity outbox) {
        JsonNode payload = outbox.payload();
        RuntimeEventResponse costConfirmed = new RuntimeEventResponse(
                outbox.eventId(),
                outbox.source(),
                "logistics",
                outbox.eventType(),
                outbox.aggregateType(),
                outbox.aggregateId(),
                text(payload, "correlationId", outbox.aggregateId()),
                text(payload, "causationId", outbox.aggregateId()),
                outboxStatus(outbox.status()),
                outboxSeverity(outbox.status()),
                "Logistics cost event prepared for Ledger",
                outbox.createdAt(),
                metadata(
                        "aggregateId", outbox.aggregateId(),
                        "routePlanId", text(payload, "routePlanId", outbox.aggregateId()),
                        "shipmentId", text(payload, "shipmentId", null),
                        "orderId", text(payload, "orderId", null),
                        "workdayId", text(payload, "workdayId", null),
                        "idempotencyKey", outbox.idempotencyKey(),
                        "targetService", "Archive-Ledger",
                        "simulationRunId", text(payload, "simulationRunId", null),
                        "settlementCycleId", text(payload, "settlementCycleId", null),
                        "hopCount", text(payload, "hopCount", "0"),
                        "maxHop", text(payload, "maxHop", "5")
                )
        );
        if (outbox.status() != OutboxStatus.PUBLISHED) {
            return List.of(costConfirmed);
        }
        RuntimeEventResponse published = new RuntimeEventResponse(
                outbox.eventId() + ":LEDGER_EVENT_PUBLISHED",
                outbox.source(),
                "logistics",
                "LEDGER_EVENT_PUBLISHED",
                outbox.aggregateType(),
                outbox.aggregateId(),
                text(payload, "correlationId", outbox.aggregateId()),
                text(payload, "causationId", outbox.aggregateId()),
                "settled",
                "info",
                "Ledger logistics event published",
                outbox.publishedAt() == null ? outbox.createdAt() : outbox.publishedAt(),
                metadata(
                        "aggregateId", outbox.aggregateId(),
                        "routePlanId", text(payload, "routePlanId", outbox.aggregateId()),
                        "shipmentId", text(payload, "shipmentId", null),
                        "orderId", text(payload, "orderId", null),
                        "workdayId", text(payload, "workdayId", null),
                        "idempotencyKey", outbox.idempotencyKey(),
                        "targetService", "Archive-Ledger",
                        "simulationRunId", text(payload, "simulationRunId", null),
                        "settlementCycleId", text(payload, "settlementCycleId", null),
                        "hopCount", text(payload, "hopCount", "0"),
                        "maxHop", text(payload, "maxHop", "5")
                )
        );
        return List.of(costConfirmed, published);
    }

    private RuntimeEventResponse fromAllocation(WorkforceAllocationEntity allocation) {
        return new RuntimeEventResponse(
                allocation.allocationId(),
                allocation.sourceService(),
                "logistics",
                "WORKFORCE_ALLOCATION_ASSIGNED",
                "workforce_allocation",
                allocation.allocationId(),
                fallback(allocation.correlationId(), allocation.workdayId()),
                fallback(allocation.causationId(), allocation.workdayId()),
                "completed",
                "info",
                "Synthetic logistics workforce allocation assigned",
                allocation.createdAt(),
                metadata(
                        "workdayId", allocation.workdayId(),
                        "roleType", allocation.roleType().name(),
                        "allocatedHeadcount", allocation.allocatedHeadcount(),
                        "effectiveCapacity", allocation.effectiveCapacity(),
                        "idempotencyKey", "RUNTIME:WORKFORCE_ALLOCATION_ASSIGNED:" + allocation.allocationId(),
                        "targetService", allocation.targetService(),
                        "simulationRunId", allocation.simulationRunId(),
                        "settlementCycleId", allocation.settlementCycleId(),
                        "hopCount", allocation.hopCount(),
                        "maxHop", allocation.maxHop()
                )
        );
    }

    private List<RuntimeEventResponse> fromWorkday(WorkdayProductivityEntity workday) {
        List<RuntimeEventResponse> events = new ArrayList<>();
        events.add(new RuntimeEventResponse(
                workday.workdayId(),
                SERVICE,
                "logistics",
                "WORKDAY_COMPLETED",
                "workday",
                workday.workdayId(),
                workday.workdayId(),
                fallback(workday.allocationId(), workday.workdayId()),
                "completed",
                workday.backlogEvents() > 0 ? "warning" : "info",
                "Synthetic logistics workday completed",
                workday.createdAt(),
                metadata(
                        "shipmentsRequested", workday.shipmentsRequested(),
                        "shipmentsDispatched", workday.shipmentsDispatched(),
                        "deliveryCompleted", workday.deliveryCompleted(),
                        "shipmentsDelayed", workday.shipmentsDelayed(),
                        "bottleneckRole", workday.bottleneckType(),
                        "idempotencyKey", "RUNTIME:WORKDAY_COMPLETED:" + workday.workdayId(),
                        "targetService", "ArchiveOS",
                        "workdayId", workday.workdayId(),
                        "hopCount", 0,
                        "maxHop", 5
                )
        ));
        if (workday.shortageEvents() > 0) {
            events.add(workdaySignal(workday, "CAPACITY_SHORTAGE_DETECTED", workday.shortageEvents(), "critical"));
        }
        if (workday.backlogEvents() > 0) {
            events.add(workdaySignal(workday, "LOGISTICS_BACKLOG_INCREASED", workday.backlogEvents(), "warning"));
        }
        return events;
    }

    private RuntimeEventResponse workdaySignal(WorkdayProductivityEntity workday, String eventType, long count, String severity) {
        return new RuntimeEventResponse(
                workday.workdayId() + ":" + eventType,
                SERVICE,
                "logistics",
                eventType,
                "workday",
                workday.workdayId(),
                workday.workdayId(),
                fallback(workday.allocationId(), workday.workdayId()),
                "waiting",
                severity,
                eventType.replace('_', ' '),
                workday.createdAt(),
                metadata(
                        "count", count,
                        "bottleneckRole", workday.bottleneckType(),
                        "totalCapacity", workday.capacityEvents(),
                        "usedCapacity", workday.usedCapacity(),
                        "idempotencyKey", "RUNTIME:" + eventType + ":" + workday.workdayId(),
                        "targetService", "ArchiveOS",
                        "workdayId", workday.workdayId(),
                        "hopCount", 0,
                        "maxHop", 5
                )
        );
    }

    private String switchStatus(String status) {
        return switch (status) {
            case "PROCESSED" -> "completed";
            case "FAILED" -> "failed";
            case "DUPLICATE" -> "waiting";
            default -> "moving";
        };
    }

    private String outboxStatus(OutboxStatus status) {
        return switch (status) {
            case PUBLISHED -> "settled";
            case FAILED -> "failed";
            case RETRY -> "waiting";
            case SKIPPED -> "unavailable";
            default -> "moving";
        };
    }

    private String outboxSeverity(OutboxStatus status) {
        return switch (status) {
            case FAILED -> "critical";
            case RETRY, SKIPPED -> "warning";
            default -> "info";
        };
    }

    private String syntheticLocationType(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        if (code.startsWith("FAC-")) {
            return "FACTORY";
        }
        if (code.startsWith("DC-")) {
            return "DISTRIBUTION_CENTER";
        }
        if (code.startsWith("VENDOR-")) {
            return "SYNTHETIC_VENDOR";
        }
        return "SYNTHETIC_LOCATION";
    }

    private String text(JsonNode node, String field, String fallback) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return fallback;
        }
        String value = node.path(field).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Map<String, Object> metadata(Object... values) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            Object value = values[index + 1];
            if (value != null) {
                metadata.put(String.valueOf(values[index]), value);
            }
        }
        return metadata;
    }
}
