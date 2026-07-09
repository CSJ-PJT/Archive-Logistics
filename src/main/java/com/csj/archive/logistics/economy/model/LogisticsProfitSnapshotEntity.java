package com.csj.archive.logistics.economy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "logistics_profit_snapshot")
public class LogisticsProfitSnapshotEntity {
    @Id
    @Column(name = "snapshot_id", nullable = false, length = 100)
    private String snapshotId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "revenue_amount", nullable = false)
    private long revenueAmount;

    @Column(name = "cost_amount", nullable = false)
    private long costAmount;

    @Column(name = "profit_amount", nullable = false)
    private long profitAmount;

    @Column(name = "cash_balance", nullable = false)
    private long cashBalance;

    @Column(name = "bankruptcy_risk", nullable = false, length = 20)
    private String bankruptcyRisk;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LogisticsProfitSnapshotEntity() {
    }

    public LogisticsProfitSnapshotEntity(String snapshotId,
                                        LocalDate settlementDate,
                                        long revenueAmount,
                                        long costAmount,
                                        long profitAmount,
                                        long cashBalance,
                                        String bankruptcyRisk,
                                        LocalDateTime createdAt) {
        this.snapshotId = snapshotId;
        this.settlementDate = settlementDate;
        this.revenueAmount = revenueAmount;
        this.costAmount = costAmount;
        this.profitAmount = profitAmount;
        this.cashBalance = cashBalance;
        this.bankruptcyRisk = bankruptcyRisk;
        this.createdAt = createdAt;
    }

    public String snapshotId() {
        return snapshotId;
    }

    public LocalDate settlementDate() {
        return settlementDate;
    }

    public long revenueAmount() {
        return revenueAmount;
    }

    public long costAmount() {
        return costAmount;
    }

    public long profitAmount() {
        return profitAmount;
    }

    public long cashBalance() {
        return cashBalance;
    }

    public String bankruptcyRisk() {
        return bankruptcyRisk;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}

