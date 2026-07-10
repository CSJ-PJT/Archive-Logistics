package com.csj.archive.logistics.operations;

public record OperationsSummaryResponse(
        String service,
        String status,
        String profile,
        long receivedEvents,
        long processedEvents,
        long duplicateEvents,
        long failedEvents,
        long routePlans,
        Economy economy,
        Outbox outbox,
        Risk risk,
        MarketOrigin marketOrigin,
        Ledger ledger,
        Memory memory
) {
    public record Economy(long totalRevenue, long totalCost, long totalProfit, long cashBalance, String bankruptcyRisk) {
    }
    public record Outbox(long pending, long published, long failed, long retry, long skipped) {
    }

    public record Risk(long approvalRequired, long delayedRoutes, long deviatedRoutes, long coldChainRisk) {
    }
    public record MarketOrigin(
            long marketOriginRoutes,
            long expressOrderRoutes,
            long vipCustomerRoutes,
            long highRiskCustomerRoutes
    ) {
    }

    public record Ledger(boolean enabled, String status, String baseUrl, String bulkEndpoint, String contractMode) {
    }

    public record Memory(long maxHeapMb, long usedHeapMb) {
    }
}
