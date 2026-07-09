package com.csj.archive.logistics.outbox;

public record OutboxPublishResult(
        String batchId,
        boolean ledgerEnabled,
        boolean dryRun,
        String endpoint,
        String contractMode,
        int requestedCount,
        int publishedCount,
        int failedCount,
        int retriedCount,
        int skippedCount,
        String status,
        String message
) {
}
