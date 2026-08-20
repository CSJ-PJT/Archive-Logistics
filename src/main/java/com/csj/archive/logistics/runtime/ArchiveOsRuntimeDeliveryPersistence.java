package com.csj.archive.logistics.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ArchiveOsRuntimeDeliveryPersistence {
    private final ArchiveOsRuntimeDeliveryRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;
    public ArchiveOsRuntimeDeliveryPersistence(ArchiveOsRuntimeDeliveryRepository repository, ObjectMapper mapper, Clock clock) { this.repository = repository; this.mapper = mapper; this.clock = clock; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(ArchiveOsRuntimeDeliveryService.Projection projection) {
        if (repository.existsByEventId(projection.eventId())) return;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceSystem", "archive-logistics"); payload.put("targetSystem", "archiveos");
        payload.put("eventId", projection.eventId()); payload.put("correlationId", projection.correlationId());
        payload.put("causationId", projection.causationId()); payload.put("orderId", projection.orderId());
        payload.put("entityType", projection.entityType()); payload.put("entityId", projection.entityId()); payload.put("eventType", projection.eventType());
        payload.put("occurredAt", projection.occurredAt().atZone(ZoneOffset.UTC).toInstant().toString());
        payload.put("status", projection.status()); payload.put("severity", projection.severity()); payload.put("simulationRunId", projection.simulationRunId());
        payload.put("metadata", projection.metadata() == null ? Map.of() : projection.metadata());
        repository.save(new ArchiveOsRuntimeDeliveryEntity(projection.eventId(), projection.idempotencyKey(), projection.correlationId(),
                projection.causationId(), projection.orderId(), projection.simulationRunId(), projection.entityType(), projection.entityId(),
                projection.eventType(), mapper.valueToTree(payload), LocalDateTime.now(clock)));
    }
}
