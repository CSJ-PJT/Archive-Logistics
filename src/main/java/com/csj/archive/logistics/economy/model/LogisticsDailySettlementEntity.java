package com.csj.archive.logistics.economy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "logistics_daily_settlement")
public class LogisticsDailySettlementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false, unique = true, length = 100)
    private String settlementId;

    @Column(name = "settlement_cycle_id", nullable = false, length = 100)
    private String settlementCycleId;

    @Column(name = "settled_at", nullable = false)
    private LocalDate settledAt;

    @Column(name = "billed_to_service", nullable = false, length = 100)
    private String billedToService;

    @Column(name = "route_count", nullable = false)
    private long routeCount;

    @Column(name = "total_delivery_fee", nullable = false)
    private long totalDeliveryFee;

    @Column(name = "total_surcharge", nullable = false)
    private long totalSurcharge;

    @Column(name = "total_cost", nullable = false)
    private long totalCost;

    @Column(name = "ledger_fee_paid", nullable = false)
    private long ledgerFeePaid;

    @Column(name = "net_profit", nullable = false)
    private long netProfit;

    @Column(name = "factory_id", nullable = false, length = 50)
    private String factoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LogisticsSettlementStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected LogisticsDailySettlementEntity() {
    }

    public LogisticsDailySettlementEntity(String settlementId,
                                         String settlementCycleId,
                                         LocalDate settledAt,
                                         String billedToService,
                                         String factoryId,
                                         long routeCount,
                                         long totalDeliveryFee,
                                         long totalSurcharge,
                                         long totalCost,
                                         long ledgerFeePaid,
                                         long netProfit,
                                         LogisticsSettlementStatus status,
                                         LocalDateTime createdAt) {
        this.settlementId = settlementId;
        this.settlementCycleId = settlementCycleId;
        this.settledAt = settledAt;
        this.billedToService = billedToService;
        this.factoryId = factoryId;
        this.routeCount = routeCount;
        this.totalDeliveryFee = totalDeliveryFee;
        this.totalSurcharge = totalSurcharge;
        this.totalCost = totalCost;
        this.ledgerFeePaid = ledgerFeePaid;
        this.netProfit = netProfit;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = status == LogisticsSettlementStatus.SENT ? createdAt : null;
    }

    public void markSent(long ledgerFeePaid, long netProfit, LocalDateTime completedAt) {
        this.ledgerFeePaid = ledgerFeePaid;
        this.netProfit = netProfit;
        this.status = LogisticsSettlementStatus.SENT;
        this.completedAt = completedAt;
    }

    public void markSkipped(LocalDateTime completedAt) {
        this.status = LogisticsSettlementStatus.SKIPPED;
        this.completedAt = completedAt;
        this.netProfit = this.totalDeliveryFee + this.totalSurcharge - this.totalCost;
    }

    public void markDryRun() {
        this.status = LogisticsSettlementStatus.DRY_RUN;
    }

    public Long id() {
        return id;
    }

    public String settlementId() {
        return settlementId;
    }

    public String settlementCycleId() {
        return settlementCycleId;
    }

    public LocalDate settledAt() {
        return settledAt;
    }

    public String billedToService() {
        return billedToService;
    }

    public String factoryId() {
        return factoryId;
    }

    public long routeCount() {
        return routeCount;
    }

    public long totalDeliveryFee() {
        return totalDeliveryFee;
    }

    public long totalSurcharge() {
        return totalSurcharge;
    }

    public long totalCost() {
        return totalCost;
    }

    public long ledgerFeePaid() {
        return ledgerFeePaid;
    }

    public long netProfit() {
        return netProfit;
    }

    public LogisticsSettlementStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime completedAt() {
        return completedAt;
    }
}

