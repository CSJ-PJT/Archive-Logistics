package com.csj.archive.logistics.ledger;

import java.util.List;

public record LedgerBulkPublishRequest(
        List<LedgerCompatibleEventPayload> events
) {
}
