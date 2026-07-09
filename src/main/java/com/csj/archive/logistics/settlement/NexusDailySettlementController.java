package com.csj.archive.logistics.settlement;

import com.csj.archive.logistics.common.ApiResponse;
import com.csj.archive.logistics.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/settlements/nexus-daily")
public class NexusDailySettlementController {
    private final NexusDailySettlementService settlementService;

    public NexusDailySettlementController(NexusDailySettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/run")
    public ApiResponse<NexusDailySettlementRunResult> run(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String factoryId) {
        return ApiResponse.ok(settlementService.run(date, factoryId));
    }

    @GetMapping
    public ApiResponse<PageResponse<NexusDailySettlementResponse>> settlements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Pageable pageable) {
        return ApiResponse.ok(settlementService.settlements(date, pageable));
    }

    @GetMapping("/summary")
    public ApiResponse<NexusDailySettlementSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(settlementService.summary(date));
    }

    @GetMapping("/{settlementId}")
    public ApiResponse<NexusDailySettlementResponse> settlement(@PathVariable String settlementId) {
        return ApiResponse.ok(settlementService.settlement(settlementId));
    }
}
