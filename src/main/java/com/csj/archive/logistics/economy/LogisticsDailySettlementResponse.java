package com.csj.archive.logistics.economy;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LogisticsDailySettlementResponse(
        String settlementId,
        String settlementCycleId,
        LocalDate settlementDate,
        String billedToService,
        String factoryId,
        long routeCount,
        long totalDeliveryFee,
        long totalSurcharge,
        long totalCost,
        long ledgerFeePaid,
        long netProfit,
        String status,
        LocalDateTime completedAt
) {
}

