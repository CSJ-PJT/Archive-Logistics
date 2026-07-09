package com.csj.archive.logistics.route;

import java.math.BigDecimal;

public record RouteSummaryResponse(
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
        Long totalCost,
        boolean requiresApproval,
        String outboxStatus
) {
}
