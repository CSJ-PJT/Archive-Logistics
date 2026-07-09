package com.csj.archive.logistics.event;

public record NexusBulkEventProcessResponse(
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
