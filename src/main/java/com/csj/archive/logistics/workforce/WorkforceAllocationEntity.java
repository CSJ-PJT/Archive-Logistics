package com.csj.archive.logistics.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

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

    @Column(name = "target_service", length = 100)
    private String targetService;

    @Column(name = "workday_id", length = 100)
    private String workdayId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "role_type", length = 80)
    private String roleType;

    @Column(name = "allocated_headcount")
    private int allocatedHeadcount;

    @Column(name = "capacity_per_person_per_day")
    private int capacityPerPersonPerDay;

    @Column(name = "productivity_score", precision = 7, scale = 4)
    private BigDecimal productivityScore;

    @Column(name = "wage_per_day")
    private long wagePerDay;

    @Column(name = "effective_capacity")
    private long effectiveCapacity;

    @Column(name = "used_capacity")
    private long usedCapacity;

    @Column(name = "remaining_capacity")
    private long remainingCapacity;

    @Column(name = "status", length = 50)
    private String status;

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

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected WorkforceAllocationEntity() {
    }

    public WorkforceAllocationEntity(String allocationId,
                                     WorkforceAllocationRequest request,
                                     LogisticsWorkforceRole role,
                                     int allocatedHeadcount,
                                     int capacityPerPersonPerDay,
                                     BigDecimal productivityScore,
                                     long wagePerDay,
                                     int fallbackMaxHop,
                                     LocalDateTime now) {
        this.allocationId = allocationId;
        this.sourceService = request.normalizedSourceService();
        this.targetService = request.normalizedTargetService();
        this.workdayId = request.workdayId();
        this.workDate = request.workDate();
        this.roleType = role.name();
        this.allocatedHeadcount = allocatedHeadcount;
        this.capacityPerPersonPerDay = capacityPerPersonPerDay;
        this.productivityScore = productivityScore;
        this.wagePerDay = wagePerDay;
        this.effectiveCapacity = BigDecimal.valueOf((long) allocatedHeadcount * capacityPerPersonPerDay)
                .multiply(productivityScore)
                .longValue();
        this.usedCapacity = 0L;
        this.remainingCapacity = this.effectiveCapacity;
        this.status = "ASSIGNED";
        this.simulationRunId = request.simulationRunId();
        this.settlementCycleId = request.settlementCycleId();
        this.correlationId = request.correlationId();
        this.causationId = request.causationId();
        this.hopCount = request.hopCount() == null ? 0 : Math.max(0, request.hopCount());
        this.maxHop = request.maxHop() == null || request.maxHop() <= 0 ? fallbackMaxHop : request.maxHop();
        this.reason = request.reason();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String allocationId() {
        return allocationId;
    }

    public String sourceService() {
        return sourceService;
    }

    public String targetService() {
        return targetService;
    }

    public String workdayId() {
        return workdayId;
    }

    public LocalDate workDate() {
        return workDate;
    }

    public LogisticsWorkforceRole roleType() {
        return LogisticsWorkforceRole.valueOf(roleType);
    }

    public int allocatedHeadcount() {
        return allocatedHeadcount;
    }

    public int capacityPerPersonPerDay() {
        return capacityPerPersonPerDay;
    }

    public BigDecimal productivityScore() {
        return productivityScore;
    }

    public long wagePerDay() {
        return wagePerDay;
    }

    public long effectiveCapacity() {
        return effectiveCapacity;
    }

    public long usedCapacity() {
        return usedCapacity;
    }

    public long remainingCapacity() {
        return remainingCapacity;
    }

    public String status() {
        return status;
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
