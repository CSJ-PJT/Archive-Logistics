package com.csj.archive.logistics.economy;

import java.time.LocalDate;
import java.util.List;

public record LogisticsDailySettlementRunResult(
        LocalDate settlementDate,
        int requestedFactoryCount,
        int sentCount,
        int skippedCount,
        int failedCount,
        int totalSettlements,
        int publishedCount,
        List<LogisticsDailySettlementResponse> settlements
) {
}

