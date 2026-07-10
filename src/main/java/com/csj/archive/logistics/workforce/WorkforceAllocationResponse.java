package com.csj.archive.logistics.workforce;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WorkforceAllocationResponse(
        String workdayId,
        LocalDate workDate,
        String sourceService,
        String targetService,
        List<RoleAllocationResponse> roles,
        long totalEffectiveCapacity,
        long payrollCost,
        String simulationRunId,
        String settlementCycleId,
        String correlationId,
        String causationId
) {
    static WorkforceAllocationResponse from(List<WorkforceAllocationEntity> entities) {
        WorkforceAllocationEntity first = entities.getFirst();
        return new WorkforceAllocationResponse(
                first.workdayId(),
                first.workDate(),
                first.sourceService(),
                first.targetService(),
                entities.stream().map(RoleAllocationResponse::from).toList(),
                entities.stream().mapToLong(WorkforceAllocationEntity::effectiveCapacity).sum(),
                entities.stream().mapToLong(entity -> (long) entity.allocatedHeadcount() * entity.wagePerDay()).sum(),
                first.simulationRunId(),
                first.settlementCycleId(),
                first.correlationId(),
                first.causationId()
        );
    }

    public record RoleAllocationResponse(
            String allocationId,
            LogisticsWorkforceRole roleType,
            int allocatedHeadcount,
            int capacityPerPersonPerDay,
            BigDecimal productivityScore,
            long wagePerDay,
            long effectiveCapacity,
            long usedCapacity,
            long remainingCapacity,
            String status
    ) {
        static RoleAllocationResponse from(WorkforceAllocationEntity entity) {
            return new RoleAllocationResponse(
                    entity.allocationId(),
                    entity.roleType(),
                    entity.allocatedHeadcount(),
                    entity.capacityPerPersonPerDay(),
                    entity.productivityScore(),
                    entity.wagePerDay(),
                    entity.effectiveCapacity(),
                    entity.usedCapacity(),
                    entity.remainingCapacity(),
                    entity.status()
            );
        }
    }
}
