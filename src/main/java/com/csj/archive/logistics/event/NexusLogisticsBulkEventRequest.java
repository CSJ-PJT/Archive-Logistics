package com.csj.archive.logistics.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NexusLogisticsBulkEventRequest(
        @NotEmpty
        @Size(max = 10_000)
        List<@Valid NexusLogisticsEventRequest> events
) {
}
