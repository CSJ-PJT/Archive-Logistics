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
@Table(name = "logistics_revenue_event")
public class LogisticsRevenueEventEntity {
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

    @Column(name = "billed_to_service", nullable = false, length = 100)
    private String billedToService;

    @Enumerated(EnumType.STRING)
    @Column(name = "revenue_type", nullable = false, length = 80)
    private LogisticsRevenueType revenueType;

    @Column(name = "base_amount", nullable = false)
    private long baseAmount;

    @Column(name = "revenue_amount", nullable = false)
    private long revenueAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LogisticsRevenueEventEntity() {
    }

    public LogisticsRevenueEventEntity(
            String eventId,
            String idempotencyKey,
            String simulationRunId,
            String settlementCycleId,
            String sourceService,
            String billedToService,
            LogisticsRevenueType revenueType,
            long baseAmount,
            long revenueAmount,
            String currency,
            String reason,
            LocalDateTime createdAt) {
        this.eventId = eventId;
        this.idempotencyKey = idempotencyKey;
        this.simulationRunId = simulationRunId;
        this.settlementCycleId = settlementCycleId;
        this.sourceService = sourceService;
        this.billedToService = billedToService;
        this.revenueType = revenueType;
        this.baseAmount = baseAmount;
        this.revenueAmount = revenueAmount;
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

    public String billedToService() {
        return billedToService;
    }

    public LogisticsRevenueType revenueType() {
        return revenueType;
    }

    public long baseAmount() {
        return baseAmount;
    }

    public long revenueAmount() {
        return revenueAmount;
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
