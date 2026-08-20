package com.csj.archive.logistics.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ArchiveOsRuntimeDeliveryService {
    private static final Set<String> ALLOWED = Set.of("syntheticHubId", "destinationType", "shipmentId", "routePlanId", "orderId", "priority", "productType", "estimatedMinutes", "riskScore", "backlogCount", "bottleneckRole", "workdayId", "totalCost", "currency", "requiresApproval");
    private final ArchiveOsRuntimeDeliveryRepository repository;
    private final ArchiveOsRuntimeDeliveryPersistence persistence;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final ArchiveOsRuntimePublisher publisher;
    private final ArchiveOsRuntimeProperties properties;
    private final Set<String> activelyPublishing = ConcurrentHashMap.newKeySet();

    public ArchiveOsRuntimeDeliveryService(ArchiveOsRuntimeDeliveryRepository repository, ArchiveOsRuntimeDeliveryPersistence persistence,
                                           ObjectMapper mapper, Clock clock, ArchiveOsRuntimePublisher publisher,
                                           ArchiveOsRuntimeProperties properties) {
        this.repository = repository; this.persistence = persistence; this.mapper = mapper; this.clock = clock;
        this.publisher = publisher; this.properties = properties;
    }

    public void snapshot(ShipmentRuntimeEventEntity event) {
        Map<String, Object> metadata = metadata(event.metadata());
        schedule(new Projection(event.eventId(), event.idempotencyKey(), event.correlationId(), event.causationId(),
                value(metadata, "orderId"), event.simulationRunId(), "shipment", event.shipmentId(), event.eventType(),
                event.occurredAt(), event.status(), event.severity(), metadata));
    }

    public void schedule(Projection projection) {
        if (projection == null || blank(projection.eventId()) || blank(projection.idempotencyKey()) || blank(projection.correlationId())
                || blank(projection.entityType()) || blank(projection.entityId()) || blank(projection.eventType())) return;
        Runnable save = () -> { try { persistence.persist(projection); } catch (RuntimeException ignored) { /* observability is isolated */ } };
        if (TransactionSynchronizationManager.isSynchronizationActive() && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { @Override public void afterCommit() { save.run(); } });
        } else save.run();
    }

    public long pending() { return repository.countByStatus(ArchiveOsDeliveryStatus.PENDING); }
    public List<ArchiveOsRuntimeDeliveryEntity> correlation(String id) { return repository.findByCorrelationId(id); }

    @Transactional
    public void publishBatch() {
        for (ArchiveOsRuntimeDeliveryEntity delivery : repository.findByStatusIn(List.of(ArchiveOsDeliveryStatus.PENDING, ArchiveOsDeliveryStatus.RETRY_WAIT), PageRequest.of(0, properties.getBatchSize()))) {
            if (!activelyPublishing.add(delivery.eventId())) continue;
            try {
                delivery.publishing(LocalDateTime.now(clock));
                ArchiveOsRuntimePublisher.Result result = publisher.publish(delivery.payload());
                if (result.status() == ArchiveOsDeliveryStatus.PUBLISHED) delivery.published(LocalDateTime.now(clock));
                else if (delivery.retryCount() + 1 >= properties.getMaxRetryCount() && result.retryable()) delivery.fail(ArchiveOsDeliveryStatus.FAILED, result.code(), result.code(), null);
                else delivery.fail(result.status(), result.code(), result.code(), result.retryable() ? LocalDateTime.now(clock).plusSeconds(30) : null);
            } finally { activelyPublishing.remove(delivery.eventId()); }
        }
    }

    @Transactional
    public int recoverStalePublishing() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusSeconds(properties.getStalePublishingSeconds());
        int recovered = 0;
        for (ArchiveOsRuntimeDeliveryEntity delivery : repository.findStalePublishing(ArchiveOsDeliveryStatus.PUBLISHING, cutoff, PageRequest.of(0, properties.getBatchSize()))) {
            if (activelyPublishing.contains(delivery.eventId())) continue;
            delivery.recoverStale(LocalDateTime.now(clock), properties.getMaxRetryCount());
            recovered++;
        }
        return recovered;
    }

    private Map<String, Object> metadata(com.fasterxml.jackson.databind.JsonNode source) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source != null) source.fields().forEachRemaining(entry -> { if (ALLOWED.contains(entry.getKey()) && !entry.getValue().isNull()) result.put(entry.getKey(), mapper.convertValue(entry.getValue(), Object.class)); });
        return result;
    }
    private String value(Map<String, Object> map, String key) { Object value = map.get(key); return value == null ? null : String.valueOf(value); }
    private boolean blank(String value) { return value == null || value.isBlank(); }

    public record Projection(String eventId, String idempotencyKey, String correlationId, String causationId, String orderId,
                             String simulationRunId, String entityType, String entityId, String eventType, LocalDateTime occurredAt,
                             String status, String severity, Map<String, Object> metadata) { }
}
