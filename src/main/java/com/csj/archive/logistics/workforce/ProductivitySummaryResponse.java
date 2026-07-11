package com.csj.archive.logistics.workforce;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductivitySummaryResponse(
        String service,
        LocalDate workDate,
        long processedEvents,
        long capacityEvents,
        BigDecimal productivityRate,
        BigDecimal utilizationRate,
        long delayedResponseLoad,
        String status,
        boolean available,
        String reason
) {
    public ProductivitySummaryResponse(
            String service,
            LocalDate workDate,
            long processedEvents,
            long capacityEvents,
            BigDecimal productivityRate,
            BigDecimal utilizationRate,
            long delayedResponseLoad,
            String status
    ) {
        this(service, workDate, processedEvents, capacityEvents, productivityRate, utilizationRate,
                delayedResponseLoad, status, true, null);
    }
}
