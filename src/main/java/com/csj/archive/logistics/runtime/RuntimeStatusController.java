package com.csj.archive.logistics.runtime;

import com.csj.archive.logistics.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime")
public class RuntimeStatusController {
    private final RuntimeWorkLoop runtimeWorkLoop;

    public RuntimeStatusController(RuntimeWorkLoop runtimeWorkLoop) {
        this.runtimeWorkLoop = runtimeWorkLoop;
    }

    @GetMapping("/status")
    public ApiResponse<RuntimeStatusResponse> status() {
        return ApiResponse.ok(runtimeWorkLoop.status());
    }
}
