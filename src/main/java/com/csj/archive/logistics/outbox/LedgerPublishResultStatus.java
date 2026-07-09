package com.csj.archive.logistics.outbox;

public enum LedgerPublishResultStatus {
    SUCCESS,
    PARTIAL_FAILURE,
    FAILED,
    DRY_RUN,
    SKIPPED
}
