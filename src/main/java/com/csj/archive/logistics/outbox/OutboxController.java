package com.csj.archive.logistics.outbox;

import com.csj.archive.logistics.common.ApiResponse;
import com.csj.archive.logistics.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/outbox")
public class OutboxController {
    private final OutboxService outboxService;

    public OutboxController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @GetMapping("/events")
    public ApiResponse<PageResponse<OutboxEventResponse>> events(@RequestParam(required = false) OutboxStatus status,
                                                                 Pageable pageable) {
        return ApiResponse.ok(outboxService.events(status, pageable));
    }

    @GetMapping("/events/{eventId}")
    public ApiResponse<OutboxEventResponse> event(@PathVariable String eventId) {
        return ApiResponse.ok(outboxService.event(eventId));
    }

    @GetMapping("/summary")
    public ApiResponse<OutboxSummaryResponse> summary() {
        return ApiResponse.ok(outboxService.summary());
    }

    @PostMapping("/publish")
    public ApiResponse<OutboxPublishResult> publish() {
        return ApiResponse.ok(outboxService.publish());
    }

    @PostMapping("/retry-failed")
    public ApiResponse<Map<String, Integer>> retryFailed() {
        return ApiResponse.ok(Map.of("retryScheduledCount", outboxService.retryFailed()));
    }
}
