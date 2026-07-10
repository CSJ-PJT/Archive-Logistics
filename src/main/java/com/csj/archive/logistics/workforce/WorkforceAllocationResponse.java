package com.csj.archive.logistics.workforce;

import java.time.LocalDate;

public record WorkforceAllocationResponse(
        String allocationId,
        String sourceService,
        String workdayId,
        LocalDate workDate,
        int dispatchers,
        int drivers,
        int delayResponders,
        long syntheticDailyLaborCost,
        String simulationRunId,
        String settlementCycleId,
        String correlationId,
        String causationId,
        int hopCount,
        int maxHop,
        String reason
) {
    static WorkforceAllocationResponse from(WorkforceAllocationEntity entity) {
        return new WorkforceAllocationResponse(
                entity.allocationId(),
                entity.sourceService(),
                entity.workdayId(),
                entity.workDate(),
                entity.dispatchers(),
                entity.drivers(),
                entity.delayResponders(),
                entity.syntheticDailyLaborCost(),
                entity.simulationRunId(),
                entity.settlementCycleId(),
                entity.correlationId(),
                entity.causationId(),
                entity.hopCount(),
                entity.maxHop(),
                entity.reason()
        );
    }
}
