package com.csj.archive.logistics.workforce;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WorkforceAllocationRequest(
        String sourceService,
        String targetService,
        LocalDate workDate,
        String workdayId,
        List<RoleAllocation> roles,
        @Min(0) @Max(10_000) Integer dispatchers,
        @Min(0) @Max(10_000) Integer routePlanners,
        @Min(0) @Max(10_000) Integer drivers,
        @Min(0) @Max(10_000) Integer delayResponders,
        @Min(0) @Max(10_000) Integer coldChainHandlers,
        @Min(0) @Max(10_000) Integer logisticsManagers,
        String simulationRunId,
        String settlementCycleId,
        String correlationId,
        String causationId,
        Integer hopCount,
        Integer maxHop,
        String reason
) {
    String normalizedSourceService() {
        if (sourceService == null || sourceService.isBlank()) {
            return "ArchiveOS";
        }
        return sourceService;
    }

    String normalizedTargetService() {
        if (targetService == null || targetService.isBlank()) {
            return "Archive-Logistics";
        }
        return targetService;
    }

    public record RoleAllocation(
            LogisticsWorkforceRole roleType,
            @Min(0) @Max(10_000) int allocatedHeadcount,
            @Min(1) @Max(100_000) Integer capacityPerPersonPerDay,
            BigDecimal productivityScore,
            @Min(0) Long wagePerDay
    ) {
    }
}
