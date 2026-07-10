package com.csj.archive.logistics.workforce;

import java.time.LocalDate;

public record WorkforceSummaryResponse(
        String service,
        boolean workforceEnabled,
        boolean baselineCapacity,
        LocalDate workDate,
        String workdayId,
        String allocationId,
        int dispatchers,
        int drivers,
        int delayResponders,
        long capacityEvents,
        long usedCapacity,
        long remainingCapacity,
        long workloadEvents,
        long shipmentsRequested,
        long shipmentsDispatched,
        long shipmentsDelayed,
        long routePlansCreated,
        long deliveryCompleted,
        long processedEvents,
        long backlogEvents,
        long shortageEvents,
        long syntheticLaborCost,
        String status,
        String bottleneckType
) {
}
