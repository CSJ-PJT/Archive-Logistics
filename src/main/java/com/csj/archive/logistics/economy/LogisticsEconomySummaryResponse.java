package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.economy.model.LogisticsProfitSnapshotEntity;
import com.csj.archive.logistics.workforce.WorkforceSummaryResponse;

import java.time.LocalDateTime;
import java.time.Instant;

public record LogisticsEconomySummaryResponse(
        long totalRevenue,
        long totalCost,
        long totalProfit,
        long cashBalance,
        String bankruptcyRisk,
        String latestSnapshotId,
        Workforce workforce,
        LogisticsBalanceSummaryResponse balance,
        String currency,
        String scope,
        String window,
        LocalDateTime asOf,
        long recognizedRevenue,
        long realizedOperatingCost,
        long operatingProfit,
        String calculationScope,
        Instant periodStart,
        Instant periodEnd,
        boolean dataAvailable,
        Instant sourceLatestEventAt,
        String legacyCalculationScope
) {
    private static final String SYNTHETIC_CURRENCY = "SYNTHETIC_KRW";
    private static final String ACCOUNTING_SCOPE = "ROLLING_24H_RECOGNIZED_NON_RUNTIME_LOGISTICS_EVENTS";
    private static final String ACCOUNTING_WINDOW = "LAST_24_HOURS";
    private static final String LEGACY_SCOPE = "LIFETIME_PERSISTED_LOGISTICS_SNAPSHOT_FIELDS";

    public static LogisticsEconomySummaryResponse from(
            LogisticsProfitSnapshotEntity snapshot,
            long totalRevenue,
            long totalCost,
            long recognizedRevenue,
            long realizedOperatingCost,
            LocalDateTime asOf,
            Instant periodStart,
            Instant periodEnd,
            Instant sourceLatestEventAt
    ) {
        long operatingProfit = recognizedRevenue - realizedOperatingCost;
        boolean dataAvailable = sourceLatestEventAt != null;
        if (snapshot == null) {
            return new LogisticsEconomySummaryResponse(
                    totalRevenue, totalCost, totalRevenue - totalCost, 0L, "UNKNOWN", null, null, null,
                    SYNTHETIC_CURRENCY, ACCOUNTING_SCOPE, ACCOUNTING_WINDOW, asOf,
                    recognizedRevenue, realizedOperatingCost, operatingProfit, ACCOUNTING_SCOPE,
                    periodStart, periodEnd, dataAvailable, sourceLatestEventAt, LEGACY_SCOPE
            );
        }
        return new LogisticsEconomySummaryResponse(
                totalRevenue,
                totalCost,
                snapshot.profitAmount(),
                snapshot.cashBalance(),
                snapshot.bankruptcyRisk(),
                snapshot.snapshotId(),
                null,
                null,
                SYNTHETIC_CURRENCY,
                ACCOUNTING_SCOPE,
                ACCOUNTING_WINDOW,
                asOf,
                recognizedRevenue,
                realizedOperatingCost,
                operatingProfit,
                ACCOUNTING_SCOPE,
                periodStart,
                periodEnd,
                dataAvailable,
                sourceLatestEventAt,
                LEGACY_SCOPE
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
                balance,
                currency,
                scope,
                window,
                asOf,
                recognizedRevenue,
                realizedOperatingCost,
                operatingProfit,
                calculationScope,
                periodStart,
                periodEnd,
                dataAvailable,
                sourceLatestEventAt,
                legacyCalculationScope
        );
    }

    public LogisticsEconomySummaryResponse withBalance(LogisticsBalanceSummaryResponse value) {
        return new LogisticsEconomySummaryResponse(
                totalRevenue, totalCost, totalProfit, cashBalance, bankruptcyRisk, latestSnapshotId, workforce, value,
                currency, scope, window, asOf, recognizedRevenue, realizedOperatingCost, operatingProfit,
                calculationScope, periodStart, periodEnd, dataAvailable, sourceLatestEventAt, legacyCalculationScope
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
