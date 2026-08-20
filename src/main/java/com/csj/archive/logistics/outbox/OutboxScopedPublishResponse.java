package com.csj.archive.logistics.outbox;

import java.util.List;

/** Bounded, correlation-aware view used before an operator selects one event to publish. */
public record OutboxScopedPublishResponse(
        String correlationId,
        int selected,
        int publishable,
        int skipped,
        int alreadyPublished,
        int failed,
        List<String> eventIds,
        String blocker
) {}
