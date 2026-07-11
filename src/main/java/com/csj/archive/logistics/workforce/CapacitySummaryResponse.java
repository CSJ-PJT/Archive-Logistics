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
        String bottleneckType,
        String status,
        boolean available,
        String reason
) {
    public CapacitySummaryResponse(
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
        this(service, workDate, workforceEnabled, baselineCapacity, capacityEvents, usedCapacity,
                remainingCapacity, workloadEvents, backlogEvents, shortageEvents, bottleneckType, "AVAILABLE", true, null);
    }
}
