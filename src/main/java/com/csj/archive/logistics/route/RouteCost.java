package com.csj.archive.logistics.route;

public record RouteCost(
        long fuelCost,
        long tollCost,
        long urgentSurcharge,
        long delayPenalty,
        long coldChainPenalty,
        long totalCost,
        String currency,
        boolean requiresApproval,
        String reason
) {
}
