package com.csj.archive.logistics.simulation;

public record SimulationResult(
        int requestedCount,
        int processedCount,
        int duplicateCount,
        int failedCount,
        int outboxCreatedCount,
        int approvalRequiredCount,
        int delayedCount,
        int deviatedCount
) {
}
