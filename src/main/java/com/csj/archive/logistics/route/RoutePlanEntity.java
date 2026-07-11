package com.csj.archive.logistics.route;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "route_plan")
public class RoutePlanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_plan_id", nullable = false, unique = true, length = 100)
    private String routePlanId;

    @Column(name = "source_event_id", nullable = false, unique = true, length = 100)
    private String sourceEventId;

    @Column(name = "shipment_id", nullable = false, length = 100)
    private String shipmentId;

    @Column(name = "factory_id", nullable = false, length = 50)
    private String factoryId;

    @Column(name = "origin_code", nullable = false, length = 50)
    private String originCode;

    @Column(name = "destination_code", nullable = false, length = 50)
    private String destinationCode;

    @Column(name = "vendor_id", nullable = false, length = 100)
    private String vendorId;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "customer_type", length = 100)
    private String customerType;

    @Column(name = "product_type", length = 100)
    private String productType;

    @Column(name = "order_amount")
    private Long orderAmount;

    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(name = "market_priority", length = 30)
    private String marketPriority;

    @Column(name = "risk_level")
    private Integer riskLevel;

    @Column(name = "express_order")
    private Boolean expressOrder;

    @Column(name = "vip_customer")
    private Boolean vipCustomer;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "causation_id", length = 100)
    private String causationId;

    @Column(name = "simulation_run_id", length = 100)
    private String simulationRunId;

    @Column(name = "settlement_cycle_id", length = 100)
    private String settlementCycleId;

    @Column(name = "hop_count")
    private Integer hopCount;

    @Column(name = "max_hop")
    private Integer maxHop;

    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(name = "priority", nullable = false, length = 30)
    private String priority;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal riskScore;

    @Column(name = "delayed", nullable = false)
    private boolean delayed;

    @Column(name = "deviated", nullable = false)
    private boolean deviated;

    @Column(name = "requires_cold_chain", nullable = false)
    private boolean requiresColdChain;

    @Column(name = "route_status", nullable = false, length = 30)
    private String routeStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RoutePlanEntity() {
    }

    public RoutePlanEntity(RoutePlan routePlan, LocalDateTime now) {
        this.routePlanId = routePlan.routePlanId();
        this.sourceEventId = routePlan.sourceEventId();
        this.shipmentId = routePlan.shipmentId();
        this.factoryId = routePlan.factoryId();
        this.originCode = routePlan.originCode();
        this.destinationCode = routePlan.destinationCode();
        this.vendorId = routePlan.vendorId();
        this.orderId = routePlan.orderId();
        this.customerId = routePlan.customerId();
        this.customerType = routePlan.customerType();
        this.productType = routePlan.productType();
        this.orderAmount = routePlan.orderAmount();
        this.totalAmount = routePlan.totalAmount();
        this.marketPriority = routePlan.marketPriority();
        this.riskLevel = routePlan.riskLevel();
        this.expressOrder = routePlan.expressOrder();
        this.vipCustomer = routePlan.requiresVipCustomer();
        this.correlationId = routePlan.correlationId();
        this.causationId = routePlan.causationId();
        this.simulationRunId = routePlan.simulationRunId();
        this.settlementCycleId = routePlan.settlementCycleId();
        this.hopCount = routePlan.hopCount();
        this.maxHop = routePlan.maxHop();
        this.distanceKm = routePlan.distanceKm();
        this.estimatedMinutes = routePlan.estimatedMinutes();
        this.priority = routePlan.priority();
        this.riskScore = routePlan.riskScore();
        this.delayed = routePlan.delayed();
        this.deviated = routePlan.deviated();
        this.requiresColdChain = routePlan.requiresColdChain();
        this.routeStatus = routePlan.routeStatus();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long id() {
        return id;
    }

    public String routePlanId() {
        return routePlanId;
    }

    public String sourceEventId() {
        return sourceEventId;
    }

    public String shipmentId() {
        return shipmentId;
    }

    public String factoryId() {
        return factoryId;
    }

    public String originCode() {
        return originCode;
    }

    public String destinationCode() {
        return destinationCode;
    }

    public String vendorId() {
        return vendorId;
    }

    public String orderId() {
        return orderId;
    }

    public String customerId() {
        return customerId;
    }

    public String customerType() {
        return customerType;
    }

    public String productType() {
        return productType;
    }

    public Long orderAmount() {
        return orderAmount;
    }

    public Long totalAmount() {
        return totalAmount;
    }

    public String marketPriority() {
        return marketPriority;
    }

    public Integer riskLevel() {
        return riskLevel;
    }

    public Boolean expressOrder() {
        return expressOrder;
    }

    public Boolean vipCustomer() {
        return vipCustomer;
    }

    public String correlationId() {
        return correlationId;
    }

    public String causationId() {
        return causationId;
    }

    public String simulationRunId() {
        return simulationRunId;
    }

    public String settlementCycleId() {
        return settlementCycleId;
    }

    public Integer hopCount() {
        return hopCount;
    }

    public Integer maxHop() {
        return maxHop;
    }

    public BigDecimal distanceKm() {
        return distanceKm;
    }

    public int estimatedMinutes() {
        return estimatedMinutes;
    }

    public String priority() {
        return priority;
    }

    public BigDecimal riskScore() {
        return riskScore;
    }

    public boolean delayed() {
        return delayed;
    }

    public boolean deviated() {
        return deviated;
    }

    public boolean requiresColdChain() {
        return requiresColdChain;
    }

    public String routeStatus() {
        return routeStatus;
    }

    public void transitionTo(String nextStatus, LocalDateTime now) {
        this.routeStatus = nextStatus;
        this.updatedAt = now;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
