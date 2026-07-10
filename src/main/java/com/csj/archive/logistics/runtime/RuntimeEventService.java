package com.csj.archive.logistics.runtime;

import com.csj.archive.logistics.event.NexusEventEntity;
import com.csj.archive.logistics.event.NexusEventRepository;
import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.outbox.OutboxStatus;
import com.csj.archive.logistics.route.RoutePlanEntity;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final LogisticsOutboxRepository outboxRepository;

    public RuntimeEventService(NexusEventRepository nexusEventRepository,
                               RoutePlanRepository routePlanRepository,
                               LogisticsOutboxRepository outboxRepository) {
        this.nexusEventRepository = nexusEventRepository;
        this.routePlanRepository = routePlanRepository;
        this.outboxRepository = outboxRepository;
    }

    public List<RuntimeEventResponse> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        PageRequest page = PageRequest.of(0, safeLimit);
        List<RuntimeEventResponse> events = new ArrayList<>();
        nexusEventRepository.findAllByOrderByCreatedAtDesc(page).forEach(event -> events.add(fromNexus(event)));
        routePlanRepository.findAllByOrderByCreatedAtDesc(page).forEach(route -> events.add(fromRoute(route)));
        outboxRepository.findAllByOrderByCreatedAtDesc(page).forEach(outbox -> events.add(fromOutbox(outbox)));
        return events.stream()
                .sorted(Comparator.comparing(RuntimeEventResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(safeLimit)
                .toList();
    }

    public List<RuntimeEventResponse> byCorrelation(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return List.of();
        }
        List<RuntimeEventResponse> events = new ArrayList<>();
        routePlanRepository.findByCorrelationId(correlationId).forEach(route -> {
            events.add(fromRoute(route));
            outboxRepository.findByAggregateId(route.routePlanId()).ifPresent(outbox -> events.add(fromOutbox(outbox)));
            nexusEventRepository.findByEventId(route.sourceEventId()).ifPresent(event -> events.add(fromNexus(event)));
        });
        return sorted(events);
    }

    public List<RuntimeEventResponse> byEntity(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return List.of();
        }
        List<RuntimeEventResponse> events = new ArrayList<>();
        nexusEventRepository.findByEventId(entityId).ifPresent(event -> events.add(fromNexus(event)));
        routePlanRepository.findByRoutePlanId(entityId).ifPresent(route -> {
            events.add(fromRoute(route));
            outboxRepository.findByAggregateId(route.routePlanId()).ifPresent(outbox -> events.add(fromOutbox(outbox)));
        });
        routePlanRepository.findByShipmentId(entityId).forEach(route -> events.add(fromRoute(route)));
        outboxRepository.findByEventId(entityId).ifPresent(outbox -> events.add(fromOutbox(outbox)));
        outboxRepository.findByAggregateId(entityId).ifPresent(outbox -> events.add(fromOutbox(outbox)));
        return sorted(events);
    }

    private List<RuntimeEventResponse> sorted(List<RuntimeEventResponse> events) {
        return events.stream()
                .sorted(Comparator.comparing(RuntimeEventResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
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
                event.eventType(),
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
                        "originCode", text(payload, "originCode", null),
                        "destinationCode", text(payload, "destinationCode", null),
                        "priority", text(payload, "priority", null),
                        "orderId", text(payload, "orderId", null)
                )
        );
    }

    private RuntimeEventResponse fromRoute(RoutePlanEntity route) {
        return new RuntimeEventResponse(
                route.routePlanId(),
                SERVICE,
                "logistics",
                "ROUTE_PLAN_CREATED",
                "route_plan",
                route.routePlanId(),
                fallback(route.correlationId(), route.sourceEventId()),
                fallback(route.causationId(), route.sourceEventId()),
                route.delayed() ? "delayed" : "completed",
                route.deviated() || route.delayed() ? "warning" : "normal",
                "Synthetic route and ETA calculated",
                route.createdAt(),
                metadata(
                        "shipmentId", route.shipmentId(),
                        "factoryId", route.factoryId(),
                        "originCode", route.originCode(),
                        "destinationCode", route.destinationCode(),
                        "orderId", route.orderId(),
                        "settlementCycleId", route.settlementCycleId()
                )
        );
    }

    private RuntimeEventResponse fromOutbox(LogisticsOutboxEntity outbox) {
        JsonNode payload = outbox.payload();
        return new RuntimeEventResponse(
                outbox.eventId(),
                outbox.source(),
                "logistics",
                outbox.eventType(),
                outbox.aggregateType(),
                outbox.aggregateId(),
                text(payload, "correlationId", outbox.aggregateId()),
                text(payload, "causationId", outbox.aggregateId()),
                outboxStatus(outbox.status()),
                outbox.status() == OutboxStatus.FAILED ? "critical" : "info",
                "Ledger publish outbox event",
                outbox.createdAt(),
                metadata(
                        "aggregateId", outbox.aggregateId(),
                        "routePlanId", text(payload, "routePlanId", outbox.aggregateId()),
                        "shipmentId", text(payload, "shipmentId", null),
                        "orderId", text(payload, "orderId", null),
                        "workdayId", text(payload, "workdayId", null)
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
