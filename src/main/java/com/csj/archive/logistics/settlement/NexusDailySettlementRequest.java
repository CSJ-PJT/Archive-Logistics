package com.csj.archive.logistics.settlement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record NexusDailySettlementRequest(
        String settlementId,
        String idempotencyKey,
        String source,
        int schemaVersion,
        LocalDate settlementDate,
        String factoryId,
        String currency,
        int totalShipments,
        int delayedShipments,
        int heldShipments,
        long totalQuantity,
        long totalLogisticsCost,
        long manufacturingImpactCost,
        BigDecimal onTimeRate,
        Map<String, Object> evidence,
        Map<String, Object> payload,
        Instant occurredAt
) {
}
