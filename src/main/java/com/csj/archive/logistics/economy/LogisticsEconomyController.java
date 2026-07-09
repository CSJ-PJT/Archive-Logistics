package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.common.ApiResponse;
import com.csj.archive.logistics.common.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/logistics-economy")
public class LogisticsEconomyController {
    private final LogisticsEconomyService economyService;

    public LogisticsEconomyController(LogisticsEconomyService economyService) {
        this.economyService = economyService;
    }

    @GetMapping("/summary")
    public ApiResponse<LogisticsEconomySummaryResponse> summary() {
        return ApiResponse.ok(economyService.summary());
    }

    @GetMapping("/revenue-events")
    public ApiResponse<PageResponse<com.csj.archive.logistics.economy.model.LogisticsRevenueEventEntity>> revenueEvents(
            Pageable pageable,
            @RequestParam(required = false) String billedToService,
            @RequestParam(required = false) String settlementCycleId,
            @RequestParam(required = false) String sourceService,
            @RequestParam(required = false) String revenueType
    ) {
        return ApiResponse.ok(economyService.revenueEvents(
                pageable,
                billedToService,
                settlementCycleId,
                sourceService,
                revenueType
        ));
    }

    @GetMapping("/cost-events")
    public ApiResponse<PageResponse<com.csj.archive.logistics.economy.model.LogisticsCostEventEntity>> costEvents(
            Pageable pageable,
            @RequestParam(required = false) String paidToService,
            @RequestParam(required = false) String settlementCycleId,
            @RequestParam(required = false) String sourceService,
            @RequestParam(required = false) String costType
    ) {
        return ApiResponse.ok(economyService.costEvents(
                pageable,
                paidToService,
                settlementCycleId,
                sourceService,
                costType
        ));
    }

    @GetMapping("/profit-snapshots")
    public ApiResponse<PageResponse<LogisticsProfitSnapshotResponse>> snapshots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settlementDate,
            Pageable pageable
    ) {
        return ApiResponse.ok(economyService.snapshots(settlementDate, pageable));
    }
}
