package com.csj.archive.logistics.workforce;

import java.time.LocalDate;

public record CapacitySummaryResponse(
        String service,
        LocalDate workDate,
        boolean workforceEnabled,
        boolean baselineCapacity,
        long capacityEvents,
        long usedCapacity,
        long remainingCapacity,
        long workloadEvents,
        long backlogEvents,
        long shortageEvents,
        String bottleneckType
) {
}
