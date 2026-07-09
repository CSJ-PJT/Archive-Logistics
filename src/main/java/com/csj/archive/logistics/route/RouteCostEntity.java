package com.csj.archive.logistics.route;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "route_cost")
public class RouteCostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_plan_id", nullable = false, unique = true, length = 100)
    private String routePlanId;

    @Column(name = "fuel_cost", nullable = false)
    private long fuelCost;

    @Column(name = "toll_cost", nullable = false)
    private long tollCost;

    @Column(name = "urgent_surcharge", nullable = false)
    private long urgentSurcharge;

    @Column(name = "delay_penalty", nullable = false)
    private long delayPenalty;

    @Column(name = "cold_chain_penalty", nullable = false)
    private long coldChainPenalty;

    @Column(name = "total_cost", nullable = false)
    private long totalCost;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RouteCostEntity() {
    }

    public RouteCostEntity(String routePlanId, RouteCost routeCost, LocalDateTime now) {
        this.routePlanId = routePlanId;
        this.fuelCost = routeCost.fuelCost();
        this.tollCost = routeCost.tollCost();
        this.urgentSurcharge = routeCost.urgentSurcharge();
        this.delayPenalty = routeCost.delayPenalty();
        this.coldChainPenalty = routeCost.coldChainPenalty();
        this.totalCost = routeCost.totalCost();
        this.currency = routeCost.currency();
        this.requiresApproval = routeCost.requiresApproval();
        this.reason = routeCost.reason();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String routePlanId() {
        return routePlanId;
    }

    public long fuelCost() {
        return fuelCost;
    }

    public long tollCost() {
        return tollCost;
    }

    public long urgentSurcharge() {
        return urgentSurcharge;
    }

    public long delayPenalty() {
        return delayPenalty;
    }

    public long coldChainPenalty() {
        return coldChainPenalty;
    }

    public long totalCost() {
        return totalCost;
    }

    public String currency() {
        return currency;
    }

    public boolean requiresApproval() {
        return requiresApproval;
    }

    public String reason() {
        return reason;
    }
}
