package com.csj.archive.logistics.runtime;

import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.event.NexusLogisticsEventRequest;
import com.csj.archive.logistics.outbox.LogisticsOutboxEntity;
import com.csj.archive.logistics.route.RouteCostEntity;
import com.csj.archive.logistics.route.RoutePlanEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Projects committed route and Ledger-outbox facts; it deliberately does not use lifecycle events. */
@Service
public class ArchiveOsRouteOutboxProjectionService {
    private final ArchiveOsRuntimeDeliveryService deliveryService;
    private final IdGenerator idGenerator;

    public ArchiveOsRouteOutboxProjectionService(ArchiveOsRuntimeDeliveryService deliveryService, IdGenerator idGenerator) {
        this.deliveryService = deliveryService; this.idGenerator = idGenerator;
    }

    public void routeCreated(NexusLogisticsEventRequest request, RoutePlanEntity route, RouteCostEntity cost, LogisticsOutboxEntity outbox, LocalDateTime occurredAt) {
        String correlationId = nonBlank(route.correlationId()) ? route.correlationId() : request.eventId();
        Map<String, Object> metadata = routeMetadata(route, cost);
        schedule(request.eventId(), request.idempotencyKey(), correlationId, route.causationId(), route.orderId(), route.simulationRunId(),
                "shipment", route.shipmentId(), "SHIPMENT_CREATED", occurredAt, "CREATED", "INFO", metadata);
        schedule(idGenerator.logiticsEventId("ROUTE_ASSIGNED", route.routePlanId()), "LOGISTICS:ROUTE_ASSIGNED:" + route.routePlanId(), correlationId,
                route.causationId(), route.orderId(), route.simulationRunId(), "route_plan", route.routePlanId(), "ROUTE_ASSIGNED", occurredAt, "PROCESSING", "INFO", metadata);
        schedule(idGenerator.logiticsEventId("ROUTE_COST_CALCULATED", route.routePlanId()), "LOGISTICS:ROUTE_COST_CALCULATED:" + route.routePlanId(), correlationId,
                route.causationId(), route.orderId(), route.simulationRunId(), "route_cost", cost.routePlanId(), "ROUTE_COST_CALCULATED", occurredAt, "COMPLETED",
                route.riskScore().doubleValue() >= .85 ? "WARNING" : "INFO", metadata);
        schedule(outbox.eventId(), outbox.idempotencyKey(), correlationId, route.causationId(), route.orderId(), route.simulationRunId(),
                "ledger_outbox", outbox.eventId(), "LOGISTICS_COST_CONFIRMED", occurredAt, "PENDING", "INFO", metadata);
    }

    public void ledgerPublished(LogisticsOutboxEntity outbox, LocalDateTime occurredAt) {
        String correlationId = text(outbox.payload(), "correlationId");
        if (!nonBlank(correlationId)) correlationId = outbox.eventId();
        String eventId = idGenerator.logiticsEventId("LEDGER_EVENT_PUBLISHED", outbox.eventId());
        Map<String, Object> metadata = new LinkedHashMap<>();
        copy(outbox, metadata, "routePlanId"); copy(outbox, metadata, "shipmentId"); copy(outbox, metadata, "orderId"); copy(outbox, metadata, "priority"); copy(outbox, metadata, "productType");
        schedule(eventId, "LOGISTICS:LEDGER_EVENT_PUBLISHED:" + outbox.eventId(), correlationId, text(outbox.payload(), "causationId"), text(outbox.payload(), "orderId"),
                text(outbox.payload(), "simulationRunId"), "ledger_outbox", outbox.eventId(), "LEDGER_EVENT_PUBLISHED", occurredAt, "PUBLISHED", "INFO", metadata);
    }

    private void schedule(String eventId, String idempotencyKey, String correlationId, String causationId, String orderId, String simulationRunId,
                          String entityType, String entityId, String eventType, LocalDateTime occurredAt, String status, String severity, Map<String, Object> metadata) {
        deliveryService.schedule(new ArchiveOsRuntimeDeliveryService.Projection(eventId, idempotencyKey, correlationId, causationId, orderId,
                simulationRunId, entityType, entityId, eventType, occurredAt, status, severity, metadata));
    }
    private Map<String, Object> routeMetadata(RoutePlanEntity route, RouteCostEntity cost) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("routePlanId", route.routePlanId()); result.put("shipmentId", route.shipmentId()); result.put("orderId", route.orderId());
        result.put("priority", route.priority()); result.put("productType", route.productType()); result.put("estimatedMinutes", route.estimatedMinutes());
        result.put("riskScore", route.riskScore()); result.put("destinationType", route.destinationCode().startsWith("DC-") ? "DISTRIBUTION_CENTER" : "SYNTHETIC_HUB");
        result.put("syntheticHubId", route.destinationCode()); result.put("totalCost", cost.totalCost()); result.put("currency", cost.currency()); result.put("requiresApproval", cost.requiresApproval());
        return result;
    }
    private void copy(LogisticsOutboxEntity event, Map<String, Object> target, String key) { String value = text(event.payload(), key); if (value != null) target.put(key, value); }
    private String text(com.fasterxml.jackson.databind.JsonNode payload, String key) { return payload == null || payload.path(key).isMissingNode() || payload.path(key).isNull() ? null : payload.path(key).asText(); }
    private boolean nonBlank(String value) { return value != null && !value.isBlank(); }
}
