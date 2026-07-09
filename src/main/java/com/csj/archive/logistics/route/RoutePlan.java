package com.csj.archive.logistics.route;

import java.math.BigDecimal;

public record RoutePlan(
        String routePlanId,
        String sourceEventId,
        String shipmentId,
        String factoryId,
        String originCode,
        String destinationCode,
        String vendorId,
        BigDecimal distanceKm,
        int estimatedMinutes,
        String priority,
        BigDecimal riskScore,
        boolean delayed,
        boolean deviated,
        boolean requiresColdChain,
        String routeStatus,
        RouteCost cost
) {
}
