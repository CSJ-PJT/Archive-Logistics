package com.csj.archive.logistics.outbox;

public record OutboxSummaryResponse(
        long pending,
        long published,
        long failed,
        long retry,
        long skipped
) {
}
