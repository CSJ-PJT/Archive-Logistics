package com.csj.archive.logistics.common;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class IdGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final Clock clock;
    private final DeterministicHash hash;

    public IdGenerator(Clock clock, DeterministicHash hash) {
        this.clock = clock;
        this.hash = hash;
    }

    public String routePlanId(String eventId, String idempotencyKey) {
        return "ROUTE-" + today() + "-" + hash.shortHash(eventId + ":" + idempotencyKey);
    }

    public String routePlanId(String eventId, String idempotencyKey, Instant occurredAt) {
        return "ROUTE-" + date(occurredAt) + "-" + hash.shortHash(eventId + ":" + idempotencyKey);
    }

    public String logiticsEventId(String ledgerEventType, String routePlanId) {
        return "evt-logitics-" + today() + "-" + hash.shortHash(ledgerEventType + ":" + routePlanId);
    }

    public String logiticsEventId(String ledgerEventType, String routePlanId, Instant occurredAt) {
        return "evt-logitics-" + date(occurredAt) + "-" + hash.shortHash(ledgerEventType + ":" + routePlanId);
    }

    public String batchId(String purpose) {
        return "BATCH-" + today() + "-" + hash.shortHash(purpose + ":" + clock.instant());
    }

    private String today() {
        return LocalDate.now(clock).format(DATE_FORMAT);
    }

    private String date(Instant occurredAt) {
        Instant value = occurredAt == null ? clock.instant() : occurredAt;
        return LocalDate.ofInstant(value, ZoneOffset.UTC).format(DATE_FORMAT);
    }
}
