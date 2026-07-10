package com.csj.archive.logistics.runtime;

import java.time.LocalDateTime;

public record RuntimeStatusResponse(
        String service,
        boolean runtimeActive,
        boolean autoRunEnabled,
        String schedulerStatus,
        LocalDateTime lastWorkAt,
        LocalDateTime lastEventAt,
        int eventsProducedLastTick,
        int eventsConsumedLastTick,
        long backlogCount,
        String pipelineStatus,
        String lastTickId,
        String lastMessage
) {
}
