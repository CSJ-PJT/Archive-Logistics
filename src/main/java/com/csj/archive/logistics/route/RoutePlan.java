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
        String orderId,
        String customerId,
        String customerType,
        String productType,
        Long orderAmount,
        Long totalAmount,
        String marketPriority,
        Integer riskLevel,
        Boolean expressOrder,
        Boolean requiresVipCustomer,
        String correlationId,
        String causationId,
        String simulationRunId,
        String settlementCycleId,
        Integer hopCount,
        Integer maxHop,
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
