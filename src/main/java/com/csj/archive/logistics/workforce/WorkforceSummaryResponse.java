package com.csj.archive.logistics.workforce;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkforceSummaryResponse(
        String service,
        String serviceName,
        boolean available,
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
        int totalHeadcount,
        long effectiveCapacity,
        long backlogCount,
        long delayedCount,
        String bottleneckRole,
        BigDecimal productivityScore,
        long payrollCost,
        LocalDateTime latestEventAt,
        String degradedReason,
        String status,
        String bottleneckType
) {
}
