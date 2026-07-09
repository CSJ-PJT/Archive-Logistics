package com.csj.archive.logistics.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    RETRY,
    SKIPPED
}
