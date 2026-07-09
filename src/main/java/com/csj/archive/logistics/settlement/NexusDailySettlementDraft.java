package com.csj.archive.logistics.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;

record NexusDailySettlementDraft(
        String settlementId,
        String idempotencyKey,
        String source,
        LocalDate settlementDate,
        String factoryId,
        String currency,
        int totalShipments,
        int delayedShipments,
        int heldShipments,
        long totalQuantity,
        long totalLogisticsCost,
        long manufacturingImpactCost,
        BigDecimal manufacturingShareRate,
        BigDecimal onTimeRate
) {
}
