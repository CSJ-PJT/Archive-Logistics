package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.common.ApiResponse;
import com.csj.archive.logistics.common.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/logistics-settlements")
public class LogisticsSettlementController {
    private final LogisticsSettlementService settlementService;

    public LogisticsSettlementController(LogisticsSettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/daily/run")
    public ApiResponse<LogisticsDailySettlementRunResult> runDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String factoryId
    ) {
        return ApiResponse.ok(settlementService.run(date, factoryId));
    }

    @GetMapping
    public ApiResponse<PageResponse<LogisticsDailySettlementResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settlementDate,
            Pageable pageable
    ) {
        return ApiResponse.ok(settlementService.settlements(settlementDate, pageable));
    }

    @GetMapping("/{settlementId}")
    public ApiResponse<LogisticsDailySettlementResponse> settlement(@PathVariable String settlementId) {
        if ("summary".equals(settlementId)) {
            throw new com.csj.archive.logistics.common.NotFoundException("No logistics settlement found: summary");
        }
        return ApiResponse.ok(settlementService.settlement(settlementId));
    }

    @GetMapping("/summary")
    public ApiResponse<LogisticsDailySettlementSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settlementDate
    ) {
        return ApiResponse.ok(settlementService.summary(settlementDate));
    }
}
