package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.economy.model.LogisticsProfitSnapshotEntity;
import com.csj.archive.logistics.workforce.WorkforceSummaryResponse;

public record LogisticsEconomySummaryResponse(
        long totalRevenue,
        long totalCost,
        long totalProfit,
        long cashBalance,
        String bankruptcyRisk,
        String latestSnapshotId,
        Workforce workforce,
        LogisticsBalanceSummaryResponse balance
) {
    public static LogisticsEconomySummaryResponse from(LogisticsProfitSnapshotEntity snapshot, long totalRevenue, long totalCost) {
        if (snapshot == null) {
            return new LogisticsEconomySummaryResponse(totalRevenue, totalCost, totalRevenue - totalCost, 0L, "UNKNOWN", null, null, null);
        }
        return new LogisticsEconomySummaryResponse(
                totalRevenue,
                totalCost,
                snapshot.profitAmount(),
                snapshot.cashBalance(),
                snapshot.bankruptcyRisk(),
                snapshot.snapshotId(),
                null,
                null
        );
    }

    public LogisticsEconomySummaryResponse withWorkforce(WorkforceSummaryResponse summary) {
        return new LogisticsEconomySummaryResponse(
                totalRevenue,
                totalCost,
                totalProfit,
                cashBalance,
                bankruptcyRisk,
                latestSnapshotId,
                new Workforce(
                        summary.workdayId(),
                        summary.capacityEvents(),
                        summary.usedCapacity(),
                        summary.remainingCapacity(),
                        summary.backlogEvents(),
                        summary.syntheticLaborCost(),
                        summary.bottleneckType()
                ),
                balance
        );
    }

    public LogisticsEconomySummaryResponse withBalance(LogisticsBalanceSummaryResponse value) {
        return new LogisticsEconomySummaryResponse(
                totalRevenue, totalCost, totalProfit, cashBalance, bankruptcyRisk, latestSnapshotId, workforce, value
        );
    }

    public record Workforce(
            String workdayId,
            long totalCapacity,
            long usedCapacity,
            long remainingCapacity,
            long backlogCount,
            long payrollCost,
            String bottleneckRole
    ) {
    }
}
