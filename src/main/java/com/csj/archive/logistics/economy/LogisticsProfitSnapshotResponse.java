package com.csj.archive.logistics.economy;

import java.time.LocalDateTime;

public record LogisticsProfitSnapshotResponse(
        String snapshotId,
        java.time.LocalDate settlementDate,
        long revenueAmount,
        long costAmount,
        long profitAmount,
        long cashBalance,
        String bankruptcyRisk,
        LocalDateTime createdAt
) {
}

