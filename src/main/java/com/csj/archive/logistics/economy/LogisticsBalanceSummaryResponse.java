package com.csj.archive.logistics.economy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LogisticsBalanceSummaryResponse(
        boolean available,
        String status,
        String reason,
        Long logisticsRevenue,
        Long fuelCost,
        Long tollCost,
        Long workforceCost,
        Long delayPenaltyCost,
        Long coldChainCost,
        Long ledgerFee,
        Long totalCost,
        Long operatingProfit,
        BigDecimal operatingMargin,
        Long cashBalance,
        Long shipmentsRequested,
        Long shipmentsDispatched,
        Long shipmentsDelayed,
        Long shipmentsCompleted,
        Long backlogCount,
        BigDecimal capacityUtilization,
        String bottleneckRole,
        BigDecimal averageEta,
        BigDecimal delayRate,
        Integer negativeProfitStreak,
        String calculationScope,
        LocalDateTime calculatedAt
) {
}
