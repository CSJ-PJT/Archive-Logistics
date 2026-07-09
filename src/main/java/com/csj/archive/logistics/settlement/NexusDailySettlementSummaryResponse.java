package com.csj.archive.logistics.settlement;

import java.time.LocalDate;

public record NexusDailySettlementSummaryResponse(
        LocalDate date,
        long totalSettlements,
        long sent,
        long dryRun,
        long retry,
        long failed,
        long totalShipments,
        long totalQuantity,
        long totalLogisticsCost,
        long totalManufacturingImpactCost
) {
}
