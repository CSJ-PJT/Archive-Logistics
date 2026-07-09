package com.csj.archive.logistics.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record NexusDailySettlementResponse(
        String settlementId,
        String idempotencyKey,
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
        BigDecimal onTimeRate,
        String status,
        int retryCount,
        String lastError,
        LocalDateTime sentAt,
        LocalDateTime nextRetryAt,
        boolean duplicate
) {
    static NexusDailySettlementResponse from(NexusDailySettlementEntity entity, boolean duplicate) {
        return new NexusDailySettlementResponse(
                entity.settlementId(),
                entity.idempotencyKey(),
                entity.settlementDate(),
                entity.factoryId(),
                entity.currency(),
                entity.totalShipments(),
                entity.delayedShipments(),
                entity.heldShipments(),
                entity.totalQuantity(),
                entity.totalLogisticsCost(),
                entity.manufacturingImpactCost(),
                entity.manufacturingShareRate(),
                entity.onTimeRate(),
                entity.status().name(),
                entity.retryCount(),
                entity.lastError(),
                entity.sentAt(),
                entity.nextRetryAt(),
                duplicate
        );
    }
}
