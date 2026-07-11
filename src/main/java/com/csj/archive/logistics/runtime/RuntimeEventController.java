package com.csj.archive.logistics.runtime;

import com.csj.archive.logistics.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/runtime-events")
public class RuntimeEventController {
    private final RuntimeEventService runtimeEventService;

    public RuntimeEventController(RuntimeEventService runtimeEventService) {
        this.runtimeEventService = runtimeEventService;
    }

    @GetMapping("/recent")
    public ApiResponse<List<RuntimeEventResponse>> recent(
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.ok(after == null || after.isBlank()
                ? runtimeEventService.recent(limit)
                : runtimeEventService.recentAfter(after, limit));
    }

    ApiResponse<List<RuntimeEventResponse>> recent(int limit) {
        return ApiResponse.ok(runtimeEventService.recent(limit));
    }

    @GetMapping("/correlation/{correlationId}")
    public ApiResponse<List<RuntimeEventResponse>> correlation(@PathVariable String correlationId) {
        return ApiResponse.ok(runtimeEventService.byCorrelation(correlationId));
    }

    @GetMapping("/entity/{entityId}")
    public ApiResponse<List<RuntimeEventResponse>> entity(@PathVariable String entityId) {
        return ApiResponse.ok(runtimeEventService.byEntity(entityId));
    }
}
