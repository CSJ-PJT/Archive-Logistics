package com.csj.archive.logistics.workforce;

import java.time.LocalDate;

public record WorkforceSummaryResponse(
        String service,
        boolean workforceEnabled,
        boolean baselineCapacity,
        LocalDate workDate,
        String allocationId,
        int dispatchers,
        int drivers,
        int delayResponders,
        long capacityEvents,
        long workloadEvents,
        long processedEvents,
        long backlogEvents,
        long shortageEvents,
        long syntheticLaborCost,
        String status,
        String bottleneckType
) {
}
