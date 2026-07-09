package com.csj.archive.logistics.event;

public record BulkEventProcessingResult(
        int requestedCount,
        int successCount,
        int duplicateCount,
        int failedCount,
        int outboxCreatedCount,
        int approvalRequiredCount,
        int delayedCount,
        int deviatedCount
) {
}
