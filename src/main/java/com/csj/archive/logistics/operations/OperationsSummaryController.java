package com.csj.archive.logistics.operations;

import com.csj.archive.logistics.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class OperationsSummaryController {
    private final OperationsSummaryService operationsSummaryService;

    public OperationsSummaryController(OperationsSummaryService operationsSummaryService) {
        this.operationsSummaryService = operationsSummaryService;
    }

    @GetMapping("/summary")
    public ApiResponse<OperationsSummaryResponse> summary() {
        return ApiResponse.ok(operationsSummaryService.summary());
    }
}
