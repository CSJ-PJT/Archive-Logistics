package com.csj.archive.logistics.event;

import com.csj.archive.logistics.common.ApiResponse;
import com.csj.archive.logistics.common.BusinessException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/nexus")
public class NexusLogisticsEventController {
    private final NexusLogisticsEventService nexusLogisticsEventService;

    public NexusLogisticsEventController(NexusLogisticsEventService nexusLogisticsEventService) {
        this.nexusLogisticsEventService = nexusLogisticsEventService;
    }

    @PostMapping
    public ApiResponse<EventProcessingResult> receive(@Valid @RequestBody NexusLogisticsEventRequest request) {
        EventProcessingResult result = nexusLogisticsEventService.process(request);
        if (result.failed()) {
            throw new BusinessException("PROCESSING_FAILED", result.failureReason(), HttpStatus.BAD_REQUEST);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/bulk")
    public ApiResponse<BulkEventProcessingResult> receiveBulk(@Valid @RequestBody NexusLogisticsBulkEventRequest request) {
        return ApiResponse.ok(nexusLogisticsEventService.processBulk(request));
    }
}
