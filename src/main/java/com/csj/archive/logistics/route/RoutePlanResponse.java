package com.csj.archive.logistics.route;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoutePlanResponse(
        String routePlanId,
        String sourceEventId,
        String shipmentId,
        String factoryId,
        String originCode,
        String destinationCode,
        BigDecimal distanceKm,
        int estimatedMinutes,
        String priority,
        BigDecimal riskScore,
        boolean delayed,
        boolean deviated,
        long totalCost,
        boolean requiresApproval,
        String outboxStatus,
        LocalDateTime createdAt
) {
}
