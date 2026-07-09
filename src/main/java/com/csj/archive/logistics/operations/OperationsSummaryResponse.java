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
        Outbox outbox,
        Risk risk,
        Ledger ledger,
        Memory memory
) {
    public record Outbox(long pending, long published, long failed, long retry, long skipped) {
    }

    public record Risk(long approvalRequired, long delayedRoutes, long deviatedRoutes, long coldChainRisk) {
    }

    public record Ledger(boolean enabled, String status, String baseUrl, String bulkEndpoint, String contractMode) {
    }

    public record Memory(long maxHeapMb, long usedHeapMb) {
    }
}
