package com.csj.archive.logistics.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "logistics_workforce_allocation")
public class WorkforceAllocationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allocation_id", nullable = false, unique = true, length = 100)
    private String allocationId;

    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;

    @Column(name = "workday_id", length = 100)
    private String workdayId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "dispatchers", nullable = false)
    private int dispatchers;

    @Column(name = "drivers", nullable = false)
    private int drivers;

    @Column(name = "delay_responders", nullable = false)
    private int delayResponders;

    @Column(name = "synthetic_daily_labor_cost", nullable = false)
    private long syntheticDailyLaborCost;

    @Column(name = "simulation_run_id", length = 100)
    private String simulationRunId;

    @Column(name = "settlement_cycle_id", length = 100)
    private String settlementCycleId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "causation_id", length = 100)
    private String causationId;

    @Column(name = "hop_count", nullable = false)
    private int hopCount;

    @Column(name = "max_hop", nullable = false)
    private int maxHop;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected WorkforceAllocationEntity() {
    }

    public WorkforceAllocationEntity(String allocationId,
                                     WorkforceAllocationRequest request,
                                     long syntheticDailyLaborCost,
                                     int fallbackMaxHop,
                                     LocalDateTime now) {
        this.allocationId = allocationId;
        this.sourceService = request.normalizedSourceService();
        this.workdayId = request.workdayId();
        this.workDate = request.workDate();
        this.dispatchers = request.dispatchers();
        this.drivers = request.drivers();
        this.delayResponders = request.delayResponders();
        this.syntheticDailyLaborCost = syntheticDailyLaborCost;
        this.simulationRunId = request.simulationRunId();
        this.settlementCycleId = request.settlementCycleId();
        this.correlationId = request.correlationId();
        this.causationId = request.causationId();
        this.hopCount = request.hopCount() == null ? 0 : Math.max(0, request.hopCount());
        this.maxHop = request.maxHop() == null || request.maxHop() <= 0 ? fallbackMaxHop : request.maxHop();
        this.reason = request.reason();
        this.createdAt = now;
    }

    public String allocationId() {
        return allocationId;
    }

    public String sourceService() {
        return sourceService;
    }

    public String workdayId() {
        return workdayId;
    }

    public LocalDate workDate() {
        return workDate;
    }

    public int dispatchers() {
        return dispatchers;
    }

    public int drivers() {
        return drivers;
    }

    public int delayResponders() {
        return delayResponders;
    }

    public long syntheticDailyLaborCost() {
        return syntheticDailyLaborCost;
    }

    public String simulationRunId() {
        return simulationRunId;
    }

    public String settlementCycleId() {
        return settlementCycleId;
    }

    public String correlationId() {
        return correlationId;
    }

    public String causationId() {
        return causationId;
    }

    public int hopCount() {
        return hopCount;
    }

    public int maxHop() {
        return maxHop;
    }

    public String reason() {
        return reason;
    }
}
