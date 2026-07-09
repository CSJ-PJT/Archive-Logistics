package com.csj.archive.logistics.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

public record NexusLogisticsEventRequest(
        @NotBlank String eventId,
        @NotBlank String idempotencyKey,
        @NotBlank String source,
        @NotBlank String eventType,
        Instant occurredAt,
        @Valid @NotNull Payload payload
) {
    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            "LOGISTICS_DISPATCHED",
            "URGENT_DELIVERY_REQUESTED",
            "SHIPMENT_HOLD_RELEASED",
            "MATERIAL_TRANSFER_REQUESTED",
            "QUALITY_REPLACEMENT_SHIPMENT"
    );

    @JsonIgnore
    @AssertTrue(message = "eventType is not supported")
    public boolean isSupportedEventType() {
        return eventType != null && SUPPORTED_EVENT_TYPES.contains(eventType);
    }

    public String occurredAtText() {
        return occurredAt == null ? null : occurredAt.toString();
    }

    public record Payload(
            @NotBlank String factoryId,
            @NotBlank String shipmentId,
            @NotBlank String originCode,
            @NotBlank String destinationCode,
            @NotBlank String priority,
            @NotBlank String itemType,
            @Min(1) int quantity,
            boolean requiresColdChain
    ) {
        public String normalizedPriority() {
            return priority == null ? "NORMAL" : priority.toUpperCase();
        }
    }
}
