package com.csj.archive.logistics.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LedgerBulkPublishResponse(
        int received,
        int accepted,
        int duplicate,
        int failed,
        List<EventResult> results
) {
    public static LedgerBulkPublishResponse success(int count) {
        return new LedgerBulkPublishResponse(count, count, 0, 0, List.of());
    }

    public int successfulCount() {
        return accepted + duplicate;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventResult(
            String eventId,
            String status,
            String transactionId,
            String message
    ) {
        public boolean successful() {
            return "ACCEPTED".equalsIgnoreCase(status) || "DUPLICATE".equalsIgnoreCase(status);
        }
    }
}
