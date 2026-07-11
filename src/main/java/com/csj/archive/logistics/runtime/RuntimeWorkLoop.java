package com.csj.archive.logistics.runtime;

import com.csj.archive.logistics.common.DeterministicHash;
import com.csj.archive.logistics.event.BulkEventProcessingResult;
import com.csj.archive.logistics.event.NexusLogisticsBulkEventRequest;
import com.csj.archive.logistics.event.NexusLogisticsEventRequest;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.outbox.OutboxStatus;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.csj.archive.logistics.workforce.WorkforceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.csj.archive.logistics.event.NexusLogisticsEventService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RuntimeWorkLoop {
    private static final Logger log = LoggerFactory.getLogger(RuntimeWorkLoop.class);
    private static final String SERVICE = "Archive-Logistics";
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String[] FACTORIES = {"FAC-A", "FAC-B", "FAC-C"};
    private static final String[] DESTINATIONS = {"DC-SEOUL-01", "DC-DAEJEON-01", "DC-BUSAN-01"};
    private static final String[] PRIORITIES = {"NORMAL", "NORMAL", "HIGH", "CRITICAL"};
    private static final String[] EVENT_TYPES = {
            "LOGISTICS_DISPATCHED",
            "URGENT_DELIVERY_REQUESTED",
            "MATERIAL_TRANSFER_REQUESTED",
            "QUALITY_REPLACEMENT_SHIPMENT"
    };

    private final RuntimeWorkLoopProperties properties;
    private final NexusLogisticsEventService nexusLogisticsEventService;
    private final WorkforceService workforceService;
    private final LogisticsOutboxRepository outboxRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RuntimeEventService runtimeEventService;
    private final ShipmentLifecycleService shipmentLifecycleService;
    private final DeterministicHash hash;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<State> state = new AtomicReference<>(State.initial());

    public RuntimeWorkLoop(RuntimeWorkLoopProperties properties,
                           NexusLogisticsEventService nexusLogisticsEventService,
                           WorkforceService workforceService,
                           LogisticsOutboxRepository outboxRepository,
                           RoutePlanRepository routePlanRepository,
                           RuntimeEventService runtimeEventService,
                           ShipmentLifecycleService shipmentLifecycleService,
                           DeterministicHash hash,
                           Clock clock) {
        this.properties = properties;
        this.nexusLogisticsEventService = nexusLogisticsEventService;
        this.workforceService = workforceService;
        this.outboxRepository = outboxRepository;
        this.routePlanRepository = routePlanRepository;
        this.runtimeEventService = runtimeEventService;
        this.shipmentLifecycleService = shipmentLifecycleService;
        this.hash = hash;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${archive.runtime.tick-interval:30s}", initialDelayString = "${archive.runtime.initial-delay:15s}")
    public void scheduledTick() {
        if (!properties.getAutorun().isEnabled()) {
            state.updateAndGet(previous -> previous.withSchedulerStatus("DISABLED", backlogCount(), latestEventAt()));
            return;
        }
        runTick();
    }

    public RuntimeTickResult runTick() {
        if (!running.compareAndSet(false, true)) {
            State current = state.get();
            state.set(current.withSchedulerStatus("SKIPPED_LOCKED", backlogCount(), latestEventAt()));
            return new RuntimeTickResult(current.lastTickId(), 0, 0, backlogCount(), "SKIPPED_LOCKED");
        }
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            String tickId = tickId(now);
            long backlog = backlogCount();
            int maxEvents = safeMaxEvents();
            int allowedEvents = backlog >= properties.getMaxBacklogPerTick()
                    ? 0
                    : (int) Math.min(maxEvents, Math.max(0L, properties.getMaxBacklogPerTick() - backlog));
            List<NexusLogisticsEventRequest> tickEvents = allowedEvents == 0 ? List.of() : events(tickId, allowedEvents, now);
            BulkEventProcessingResult result = allowedEvents == 0
                    ? new BulkEventProcessingResult(0, 0, 0, 0, 0, 0, 0, 0)
                    : nexusLogisticsEventService.processBulk(new NexusLogisticsBulkEventRequest(tickEvents));
            var workday = workforceService.runWorkday(LocalDate.now(clock));
            shipmentLifecycleService.advance(tickEvents, workday);
            long updatedBacklog = backlogCount();
            LocalDateTime latestEventAt = latestEventAt();
            String status = updatedBacklog >= properties.getMaxBacklogPerTick() ? "BACKLOG_LIMITED" : "RUNNING";
            String message = allowedEvents == 0
                    ? "Backlog guard active; workday tick updated without producing new shipment events."
                    : "Autonomous runtime tick produced synthetic logistics work.";
            state.set(new State(
                    true,
                    status,
                    now,
                    latestEventAt,
                    result.successCount(),
                    result.requestedCount(),
                    updatedBacklog,
                    updatedBacklog > 0 ? "LIVE_WITH_BACKLOG" : "LIVE",
                    tickId,
                    message
            ));
            return new RuntimeTickResult(tickId, result.successCount(), result.requestedCount(), updatedBacklog, status);
        } catch (RuntimeException exception) {
            log.warn("Autonomous runtime tick failed: {}", exception.getMessage());
            State previous = state.get();
            state.set(new State(
                    true,
                    "DEGRADED",
                    LocalDateTime.now(clock),
                    latestEventAt(),
                    0,
                    0,
                    backlogCount(),
                    "DEGRADED",
                    previous.lastTickId(),
                    exception.getMessage()
            ));
            throw exception;
        } finally {
            running.set(false);
        }
    }

    public RuntimeStatusResponse status() {
        State current = state.get();
        long backlog = backlogCount();
        LocalDateTime latestEventAt = latestEventAt();
        return new RuntimeStatusResponse(
                SERVICE,
                properties.getAutorun().isEnabled(),
                properties.getAutorun().isEnabled(),
                current.schedulerStatus(),
                current.lastWorkAt(),
                latestEventAt == null ? current.lastEventAt() : latestEventAt,
                current.eventsProducedLastTick(),
                current.eventsConsumedLastTick(),
                backlog,
                oldestBacklogAgeSeconds(),
                current.pipelineStatus(),
                current.lastTickId(),
                current.lastMessage(),
                latestCursor(),
                degradedReason(current, backlog)
        );
    }

    private List<NexusLogisticsEventRequest> events(String tickId, int count, LocalDateTime now) {
        List<NexusLogisticsEventRequest> events = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            events.add(event(tickId, index, now));
        }
        return events;
    }

    private NexusLogisticsEventRequest event(String tickId, int index, LocalDateTime now) {
        String seed = tickId + ":" + index;
        String day = now.toLocalDate().format(DAY);
        String shortHash = hash.shortHash(seed);
        String factoryId = FACTORIES[hash.bounded(seed + ":factory", FACTORIES.length)];
        String destinationCode = DESTINATIONS[hash.bounded(seed + ":destination", DESTINATIONS.length)];
        String priority = PRIORITIES[hash.bounded(seed + ":priority", PRIORITIES.length)];
        String eventType = EVENT_TYPES[hash.bounded(seed + ":type", EVENT_TYPES.length)];
        boolean coldChain = hash.bounded(seed + ":cold", 8) == 0;
        String shipmentId = "SHIP-RUNTIME-" + day + "-" + shortHash;
        String eventId = "evt-nexus-runtime-" + day + "-" + shortHash;
        String idempotencyKey = "NEXUS:RUNTIME:" + eventType + ":" + shipmentId;
        String correlationId = "CORR-RUNTIME-" + day + "-" + shortHash;
        Instant occurredAt = now.atZone(ZoneOffset.UTC).toInstant();
        return new NexusLogisticsEventRequest(
                eventId,
                idempotencyKey,
                "Archive-Nexus",
                eventType,
                occurredAt,
                new NexusLogisticsEventRequest.Payload(
                        factoryId,
                        shipmentId,
                        factoryId,
                        destinationCode,
                        priority,
                        coldChain ? "temperature-sensitive-module" : "battery-module",
                        10 + hash.bounded(seed + ":quantity", 90),
                        coldChain,
                        "ORD-RUNTIME-" + day + "-" + shortHash,
                        null,
                        hash.bounded(seed + ":vip", 5) == 0 ? "VIP_CUSTOMER" : "STANDARD_CUSTOMER",
                        "synthetic-logistics-demo",
                        null,
                        null,
                        hash.bounded(seed + ":risk", 5),
                        "CRITICAL".equals(priority) || "HIGH".equals(priority),
                        null,
                        null,
                        "SIM-RUNTIME-" + day,
                        "CYCLE-RUNTIME-" + day,
                        correlationId,
                        tickId,
                        0,
                        properties.getMaxHop(),
                        priority
                )
        );
    }

    private String tickId(LocalDateTime now) {
        long intervalSeconds = Math.max(1L, properties.getTickInterval().toSeconds());
        long bucket = now.atZone(ZoneOffset.UTC).toEpochSecond() / intervalSeconds;
        return "TICK-" + now.toLocalDate().format(DAY) + "-" + bucket;
    }

    private int safeMaxEvents() {
        return Math.max(0, Math.min(properties.getMaxEventsPerTick(), 100));
    }

    private long backlogCount() {
        return outboxRepository.countByStatusIn(List.of(OutboxStatus.PENDING, OutboxStatus.RETRY));
    }

    private LocalDateTime latestEventAt() {
        return routePlanRepository.latestCreatedAt();
    }

    private Long oldestBacklogAgeSeconds() {
        LocalDateTime oldest = outboxRepository.oldestCreatedAtByStatusIn(List.of(OutboxStatus.PENDING, OutboxStatus.RETRY));
        if (oldest == null) {
            return null;
        }
        return Math.max(0L, java.time.Duration.between(oldest, LocalDateTime.now(clock)).toSeconds());
    }

    private String latestCursor() {
        return runtimeEventService.latestCursor();
    }

    private String degradedReason(State current, long backlog) {
        if ("DEGRADED".equals(current.schedulerStatus())) {
            return current.lastMessage();
        }
        if (backlog >= properties.getMaxBacklogPerTick()) {
            return "Runtime outbox backlog guard is active.";
        }
        return null;
    }

    private record State(
            boolean runtimeActive,
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
        static State initial() {
            return new State(false, "STARTING", null, null, 0, 0, 0, "STARTING", null, null);
        }

        State withSchedulerStatus(String status, long backlogCount, LocalDateTime latestEventAt) {
            return new State(
                    runtimeActive,
                    status,
                    lastWorkAt,
                    latestEventAt == null ? lastEventAt : latestEventAt,
                    eventsProducedLastTick,
                    eventsConsumedLastTick,
                    backlogCount,
                    status.equals("DISABLED") ? "IDLE" : pipelineStatus,
                    lastTickId,
                    lastMessage
            );
        }
    }

    public record RuntimeTickResult(
            String tickId,
            int eventsProduced,
            int eventsConsumed,
            long backlogCount,
            String schedulerStatus
    ) {
    }
}
