package com.csj.archive.logistics.economy;

import java.math.BigDecimal;

public record LogisticsBalanceSummaryResponse(
        long logisticsRevenue,
        long fuelCost,
        long tollCost,
        long workforceCost,
        long delayPenaltyCost,
        long coldChainCost,
        long ledgerFee,
        long operatingProfit,
        BigDecimal operatingMargin,
        long cashBalance,
        long shipmentsRequested,
        long shipmentsDispatched,
        long shipmentsDelayed,
        long shipmentsCompleted,
        long backlogCount,
        BigDecimal capacityUtilization,
        String bottleneckRole,
        BigDecimal averageEta,
        BigDecimal delayRate,
        int negativeProfitStreak
) {
}
