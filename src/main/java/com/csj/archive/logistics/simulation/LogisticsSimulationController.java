package com.csj.archive.logistics.simulation;

import com.csj.archive.logistics.common.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/simulations")
public class LogisticsSimulationController {
    private final LogisticsSimulationService logisticsSimulationService;

    public LogisticsSimulationController(LogisticsSimulationService logisticsSimulationService) {
        this.logisticsSimulationService = logisticsSimulationService;
    }

    @PostMapping("/shipments")
    public ApiResponse<SimulationResult> createShipments(
            @RequestParam(defaultValue = "100") @Min(1) @Max(10_000) int count) {
        return ApiResponse.ok(logisticsSimulationService.simulate(count));
    }
}
