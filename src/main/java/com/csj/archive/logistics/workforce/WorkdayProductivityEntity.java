package com.csj.archive.logistics.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "logistics_workday_productivity")
public class WorkdayProductivityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workday_id", nullable = false, unique = true, length = 100)
    private String workdayId;

    @Column(name = "allocation_id", length = 100)
    private String allocationId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "workforce_enabled", nullable = false)
    private boolean workforceEnabled;

    @Column(name = "baseline_capacity", nullable = false)
    private boolean baselineCapacity;

    @Column(name = "dispatchers", nullable = false)
    private int dispatchers;

    @Column(name = "drivers", nullable = false)
    private int drivers;

    @Column(name = "delay_responders", nullable = false)
    private int delayResponders;

    @Column(name = "workload_events", nullable = false)
    private long workloadEvents;

    @Column(name = "capacity_events", nullable = false)
    private long capacityEvents;

    @Column(name = "processed_events", nullable = false)
    private long processedEvents;

    @Column(name = "backlog_events", nullable = false)
    private long backlogEvents;

    @Column(name = "shortage_events", nullable = false)
    private long shortageEvents;

    @Column(name = "delayed_response_load", nullable = false)
    private long delayedResponseLoad;

    @Column(name = "productivity_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal productivityRate;

    @Column(name = "utilization_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal utilizationRate;

    @Column(name = "synthetic_labor_cost", nullable = false)
    private long syntheticLaborCost;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "bottleneck_type", nullable = false, length = 50)
    private String bottleneckType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected WorkdayProductivityEntity() {
    }

    public WorkdayProductivityEntity(WorkdayProductivityResult result, LocalDateTime now) {
        apply(result, now);
    }

    public void apply(WorkdayProductivityResult result, LocalDateTime now) {
        this.workdayId = result.workdayId();
        this.allocationId = result.allocationId();
        this.workDate = result.workDate();
        this.workforceEnabled = result.workforceEnabled();
        this.baselineCapacity = result.baselineCapacity();
        this.dispatchers = result.dispatchers();
        this.drivers = result.drivers();
        this.delayResponders = result.delayResponders();
        this.workloadEvents = result.workloadEvents();
        this.capacityEvents = result.capacityEvents();
        this.processedEvents = result.processedEvents();
        this.backlogEvents = result.backlogEvents();
        this.shortageEvents = result.shortageEvents();
        this.delayedResponseLoad = result.delayedResponseLoad();
        this.productivityRate = result.productivityRate();
        this.utilizationRate = result.utilizationRate();
        this.syntheticLaborCost = result.syntheticLaborCost();
        this.status = result.status();
        this.bottleneckType = result.bottleneckType();
        this.createdAt = now;
    }

    public String workdayId() {
        return workdayId;
    }

    public String allocationId() {
        return allocationId;
    }

    public LocalDate workDate() {
        return workDate;
    }

    public boolean workforceEnabled() {
        return workforceEnabled;
    }

    public boolean baselineCapacity() {
        return baselineCapacity;
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

    public long workloadEvents() {
        return workloadEvents;
    }

    public long capacityEvents() {
        return capacityEvents;
    }

    public long processedEvents() {
        return processedEvents;
    }

    public long backlogEvents() {
        return backlogEvents;
    }

    public long shortageEvents() {
        return shortageEvents;
    }

    public long delayedResponseLoad() {
        return delayedResponseLoad;
    }

    public BigDecimal productivityRate() {
        return productivityRate;
    }

    public BigDecimal utilizationRate() {
        return utilizationRate;
    }

    public long syntheticLaborCost() {
        return syntheticLaborCost;
    }

    public String status() {
        return status;
    }

    public String bottleneckType() {
        return bottleneckType;
    }
}
