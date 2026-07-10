package com.csj.archive.logistics.workforce;

import com.csj.archive.logistics.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class WorkforceController {
    private final WorkforceService workforceService;

    public WorkforceController(WorkforceService workforceService) {
        this.workforceService = workforceService;
    }

    @GetMapping("/workforce/summary")
    public ApiResponse<WorkforceSummaryResponse> workforceSummary() {
        return ApiResponse.ok(workforceService.workforceSummary());
    }

    @GetMapping("/productivity/summary")
    public ApiResponse<ProductivitySummaryResponse> productivitySummary() {
        return ApiResponse.ok(workforceService.productivitySummary());
    }

    @GetMapping("/capacity/summary")
    public ApiResponse<CapacitySummaryResponse> capacitySummary() {
        return ApiResponse.ok(workforceService.capacitySummary());
    }

    @PostMapping("/workforce/allocations")
    public ApiResponse<WorkforceAllocationResponse> assign(@Valid @RequestBody WorkforceAllocationRequest request) {
        return ApiResponse.ok(workforceService.assign(request));
    }

    @PostMapping("/workforce/workday/run")
    public ApiResponse<WorkdayProductivityResult> runWorkday(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(workforceService.runWorkday(date));
    }
}
