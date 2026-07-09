package com.csj.archive.logistics.settlement;

import java.time.LocalDate;
import java.util.List;

public record NexusDailySettlementRunResult(
        LocalDate settlementDate,
        int requestedFactoryCount,
        int sentCount,
        int dryRunCount,
        int retryCount,
        int failedCount,
        int skippedCount,
        List<NexusDailySettlementResponse> settlements
) {
}
