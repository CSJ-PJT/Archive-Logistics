package com.csj.archive.logistics.route;

import java.time.LocalDate;

public record RouteAggregateSummaryResponse(
        LocalDate date,
        String factoryId,
        long routePlans,
        long delayedRoutes,
        long deviatedRoutes,
        long approvalRequired,
        long coldChainRisk,
        long totalCost
) {
}
