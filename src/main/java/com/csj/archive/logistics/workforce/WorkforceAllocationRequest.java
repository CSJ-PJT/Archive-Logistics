package com.csj.archive.logistics.workforce;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record WorkforceAllocationRequest(
        String sourceService,
        LocalDate workDate,
        String workdayId,
        @Min(0) @Max(10_000) int dispatchers,
        @Min(0) @Max(10_000) int drivers,
        @Min(0) @Max(10_000) int delayResponders,
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
}
