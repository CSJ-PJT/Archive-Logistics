package com.csj.archive.logistics.operations;

import java.time.LocalDateTime;

public record OperationsSummaryResponse(
        String service,
        String serviceName,
        String serviceRole,
        String status,
        LocalDateTime latestEventAt,
        String degradedReason,
        boolean liveFlowAvailable,
        String profile,
        long receivedEvents,
        long processedEvents,
        long duplicateEvents,
        long failedEvents,
        long routePlans,
        long shipmentsRequested,
        long shipmentsDispatched,
        long shipmentsDelayed,
        long deliveryCompleted,
        long routePlansCreated,
        long backlogCount,
        Economy economy,
        Outbox outbox,
        Risk risk,
        MarketOrigin marketOrigin,
        Workforce workforce,
        Runtime runtime,
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

    public record Workforce(
            boolean enabled,
            boolean baselineCapacity,
            long capacityEvents,
            long workloadEvents,
            long backlogEvents,
            long shortageEvents,
            long driverCapacity,
            long usedCapacity,
            String bottleneckRole,
            String status,
            String bottleneckType
    ) {
    }

    public record Runtime(
            boolean runtimeActive,
            boolean autoRunEnabled,
            String schedulerStatus,
            LocalDateTime lastWorkAt,
            LocalDateTime lastEventAt,
            int eventsProducedLastTick,
            int eventsConsumedLastTick,
            long backlogCount,
            String pipelineStatus
    ) {
    }

    public record Ledger(boolean enabled, String status, String baseUrl, String bulkEndpoint, String contractMode) {
    }

    public record Memory(long maxHeapMb, long usedHeapMb) {
    }
}
