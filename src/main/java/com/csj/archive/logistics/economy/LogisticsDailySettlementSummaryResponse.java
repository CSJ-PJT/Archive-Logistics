package com.csj.archive.logistics.economy;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LogisticsDailySettlementSummaryResponse(
        LocalDate settlementDate,
        int totalSettlements,
        long totalRoutes,
        long totalDeliveryFee,
        long totalSurcharge,
        long totalCost,
        long ledgerFee,
        long netProfit,
        BigDecimal meanNetProfitRate
) {
}

