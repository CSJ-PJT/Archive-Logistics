package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.economy.model.LogisticsProfitSnapshotEntity;

public record LogisticsEconomySummaryResponse(
        long totalRevenue,
        long totalCost,
        long totalProfit,
        long cashBalance,
        String bankruptcyRisk,
        String latestSnapshotId
) {
    public static LogisticsEconomySummaryResponse from(LogisticsProfitSnapshotEntity snapshot, long totalRevenue, long totalCost) {
        if (snapshot == null) {
            return new LogisticsEconomySummaryResponse(totalRevenue, totalCost, totalRevenue - totalCost, 0L, "UNKNOWN", null);
        }
        return new LogisticsEconomySummaryResponse(
                totalRevenue,
                totalCost,
                snapshot.profitAmount(),
                snapshot.cashBalance(),
                snapshot.bankruptcyRisk(),
                snapshot.snapshotId()
        );
    }
}

