package com.csj.archive.logistics.workforce;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkdayProductivityResult(
        String workdayId,
        String allocationId,
        LocalDate workDate,
        boolean workforceEnabled,
        boolean baselineCapacity,
        int dispatchers,
        int drivers,
        int delayResponders,
        long workloadEvents,
        long capacityEvents,
        long usedCapacity,
        long remainingCapacity,
        long shipmentsRequested,
        long shipmentsDispatched,
        long shipmentsDelayed,
        long routePlansCreated,
        long deliveryCompleted,
        long processedEvents,
        long backlogEvents,
        long shortageEvents,
        long delayedResponseLoad,
        BigDecimal productivityRate,
        BigDecimal utilizationRate,
        long syntheticLaborCost,
        String status,
        String bottleneckType
) {
    static WorkdayProductivityResult from(WorkdayProductivityEntity entity) {
        return new WorkdayProductivityResult(
                entity.workdayId(),
                entity.allocationId(),
                entity.workDate(),
                entity.workforceEnabled(),
                entity.baselineCapacity(),
                entity.dispatchers(),
                entity.drivers(),
                entity.delayResponders(),
                entity.workloadEvents(),
                entity.capacityEvents(),
                entity.usedCapacity(),
                entity.remainingCapacity(),
                entity.shipmentsRequested(),
                entity.shipmentsDispatched(),
                entity.shipmentsDelayed(),
                entity.routePlansCreated(),
                entity.deliveryCompleted(),
                entity.processedEvents(),
                entity.backlogEvents(),
                entity.shortageEvents(),
                entity.delayedResponseLoad(),
                entity.productivityRate(),
                entity.utilizationRate(),
                entity.syntheticLaborCost(),
                entity.status(),
                entity.bottleneckType()
        );
    }
}
