package com.csj.archive.logistics.ledger;

import java.util.List;

public record LedgerBulkPublishRequest(
        String source,
        List<LedgerCompatibleEventPayload> events
) {
    private static final String LEDGER_COMPAT_SOURCE = "Archive-Logitics";

    public LedgerBulkPublishRequest(List<LedgerCompatibleEventPayload> events) {
        this(LEDGER_COMPAT_SOURCE, events);
    }
}
