package com.csj.archive.logistics.event;

public record NexusEventProcessResponse(
        String eventId,
        String idempotencyKey,
        String status,
        boolean duplicate,
        String routePlanId,
        String outboxEventId,
        boolean requiresApproval,
        boolean delayed,
        boolean deviated,
        Long totalCost,
        String failureReason
) {
    public boolean failed() {
        return "FAILED".equals(status);
    }
}
