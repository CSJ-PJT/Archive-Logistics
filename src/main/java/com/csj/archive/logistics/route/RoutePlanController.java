package com.csj.archive.logistics.route;

import com.csj.archive.logistics.common.ApiResponse;
import com.csj.archive.logistics.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/routes")
public class RoutePlanController {
    private final RoutePlanService routePlanService;

    public RoutePlanController(RoutePlanService routePlanService) {
        this.routePlanService = routePlanService;
    }

    @GetMapping("/plans")
    public ApiResponse<PageResponse<RouteSummaryResponse>> plans(@RequestParam(required = false) String factoryId,
                                                                 Pageable pageable) {
        return ApiResponse.ok(routePlanService.plans(factoryId, pageable));
    }

    @GetMapping("/plans/{routePlanId}")
    public ApiResponse<RouteSummaryResponse> plan(@PathVariable String routePlanId) {
        return ApiResponse.ok(routePlanService.plan(routePlanId));
    }

    @GetMapping("/costs")
    public ApiResponse<PageResponse<RouteCostResponse>> costs(Pageable pageable) {
        return ApiResponse.ok(routePlanService.costs(pageable));
    }

    @GetMapping("/costs/{routePlanId}")
    public ApiResponse<RouteCostResponse> cost(@PathVariable String routePlanId) {
        return ApiResponse.ok(routePlanService.cost(routePlanId));
    }

    @GetMapping("/summary")
    public ApiResponse<RouteAggregateSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String factoryId) {
        return ApiResponse.ok(routePlanService.summary(date, factoryId));
    }
}
