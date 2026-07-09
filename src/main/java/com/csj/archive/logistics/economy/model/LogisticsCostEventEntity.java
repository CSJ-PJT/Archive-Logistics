package com.csj.archive.logistics.economy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "logistics_cost_event")
public class LogisticsCostEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    private String idempotencyKey;

    @Column(name = "simulation_run_id")
    private String simulationRunId;

    @Column(name = "settlement_cycle_id")
    private String settlementCycleId;

    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;

    @Column(name = "paid_to_service", nullable = false, length = 100)
    private String paidToService;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_type", nullable = false, length = 80)
    private LogisticsCostType costType;

    @Column(name = "cost_amount", nullable = false)
    private long costAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LogisticsCostEventEntity() {
    }

    public LogisticsCostEventEntity(String eventId,
                                   String idempotencyKey,
                                   String simulationRunId,
                                   String settlementCycleId,
                                   String sourceService,
                                   String paidToService,
                                   LogisticsCostType costType,
                                   long costAmount,
                                   String currency,
                                   String reason,
                                   LocalDateTime createdAt) {
        this.eventId = eventId;
        this.idempotencyKey = idempotencyKey;
        this.simulationRunId = simulationRunId;
        this.settlementCycleId = settlementCycleId;
        this.sourceService = sourceService;
        this.paidToService = paidToService;
        this.costType = costType;
        this.costAmount = costAmount;
        this.currency = currency;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Long id() {
        return id;
    }

    public String eventId() {
        return eventId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String simulationRunId() {
        return simulationRunId;
    }

    public String settlementCycleId() {
        return settlementCycleId;
    }

    public String sourceService() {
        return sourceService;
    }

    public String paidToService() {
        return paidToService;
    }

    public LogisticsCostType costType() {
        return costType;
    }

    public long costAmount() {
        return costAmount;
    }

    public String currency() {
        return currency;
    }

    public String reason() {
        return reason;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
