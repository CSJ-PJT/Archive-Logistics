package com.csj.archive.logistics.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_runtime_event")
public class ShipmentRuntimeEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 160)
    private String eventId;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 220)
    private String idempotencyKey;
    @Column(name = "route_plan_id", nullable = false, length = 100)
    private String routePlanId;
    @Column(name = "shipment_id", nullable = false, length = 100)
    private String shipmentId;
    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;
    @Column(name = "target_service", nullable = false, length = 100)
    private String targetService;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Column(name = "status", nullable = false, length = 40)
    private String status;
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;
    @Column(name = "correlation_id", length = 100)
    private String correlationId;
    @Column(name = "causation_id", length = 100)
    private String causationId;
    @Column(name = "simulation_run_id", length = 100)
    private String simulationRunId;
    @Column(name = "settlement_cycle_id", length = 100)
    private String settlementCycleId;
    @Column(name = "workday_id", length = 100)
    private String workdayId;
    @Column(name = "hop_count", nullable = false)
    private int hopCount;
    @Column(name = "max_hop", nullable = false)
    private int maxHop;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ShipmentRuntimeEventEntity() {
    }

    public ShipmentRuntimeEventEntity(String eventId, String idempotencyKey, String routePlanId, String shipmentId,
                                      String sourceService, String targetService, String eventType, String status,
                                      String severity, String correlationId, String causationId, String simulationRunId,
                                      String settlementCycleId, String workdayId, int hopCount, int maxHop,
                                      JsonNode metadata, LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.idempotencyKey = idempotencyKey;
        this.routePlanId = routePlanId;
        this.shipmentId = shipmentId;
        this.sourceService = sourceService;
        this.targetService = targetService;
        this.eventType = eventType;
        this.status = status;
        this.severity = severity;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.simulationRunId = simulationRunId;
        this.settlementCycleId = settlementCycleId;
        this.workdayId = workdayId;
        this.hopCount = hopCount;
        this.maxHop = maxHop;
        this.metadata = metadata;
        this.occurredAt = occurredAt;
        this.createdAt = occurredAt;
    }

    public String eventId() { return eventId; }
    public String idempotencyKey() { return idempotencyKey; }
    public String routePlanId() { return routePlanId; }
    public String shipmentId() { return shipmentId; }
    public String sourceService() { return sourceService; }
    public String targetService() { return targetService; }
    public String eventType() { return eventType; }
    public String status() { return status; }
    public String severity() { return severity; }
    public String correlationId() { return correlationId; }
    public String causationId() { return causationId; }
    public String simulationRunId() { return simulationRunId; }
    public String settlementCycleId() { return settlementCycleId; }
    public String workdayId() { return workdayId; }
    public int hopCount() { return hopCount; }
    public int maxHop() { return maxHop; }
    public JsonNode metadata() { return metadata; }
    public LocalDateTime occurredAt() { return occurredAt; }
}
