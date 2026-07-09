package com.csj.archive.logistics.simulation;

import com.csj.archive.logistics.audit.AuditAction;
import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.common.BusinessException;
import com.csj.archive.logistics.common.DeterministicHash;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.event.BulkEventProcessingResult;
import com.csj.archive.logistics.event.NexusLogisticsBulkEventRequest;
import com.csj.archive.logistics.event.NexusLogisticsEventRequest;
import com.csj.archive.logistics.event.NexusLogisticsEventService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LogisticsSimulationService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String[] FACTORIES = {"FAC-A", "FAC-B", "FAC-C"};
    private static final String[] DESTINATIONS = {"DC-SEOUL-01", "DC-DAEJEON-01", "DC-BUSAN-01"};
    private static final String[] PRIORITIES = {"NORMAL", "NORMAL", "NORMAL", "HIGH", "CRITICAL"};
    private static final String[] EVENT_TYPES = {
            "LOGISTICS_DISPATCHED",
            "URGENT_DELIVERY_REQUESTED",
            "SHIPMENT_HOLD_RELEASED",
            "MATERIAL_TRANSFER_REQUESTED",
            "QUALITY_REPLACEMENT_SHIPMENT"
    };

    private final NexusLogisticsEventService nexusLogisticsEventService;
    private final AuditLogService auditLogService;
    private final DeterministicHash hash;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public LogisticsSimulationService(NexusLogisticsEventService nexusLogisticsEventService,
                                      AuditLogService auditLogService,
                                      DeterministicHash hash,
                                      IdGenerator idGenerator,
                                      Clock clock) {
        this.nexusLogisticsEventService = nexusLogisticsEventService;
        this.auditLogService = auditLogService;
        this.hash = hash;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public SimulationResult simulate(int count) {
        if (count < 1 || count > 10_000) {
            throw new BusinessException("INVALID_SIMULATION_COUNT", "count must be between 1 and 10000", HttpStatus.BAD_REQUEST);
        }
        String batchId = idGenerator.batchId("simulation-" + count);
        int processed = 0;
        int duplicate = 0;
        int failed = 0;
        int outboxCreated = 0;
        int approvalRequired = 0;
        int delayed = 0;
        int deviated = 0;

        int chunkSize = 50;
        for (int offset = 0; offset < count; offset += chunkSize) {
            int to = Math.min(offset + chunkSize, count);
            List<NexusLogisticsEventRequest> events = new ArrayList<>();
            for (int index = offset; index < to; index++) {
                events.add(request(batchId, index));
            }
            BulkEventProcessingResult result = nexusLogisticsEventService.processBulk(new NexusLogisticsBulkEventRequest(events));
            processed += result.successCount();
            duplicate += result.duplicateCount();
            failed += result.failedCount();
            outboxCreated += result.outboxCreatedCount();
            approvalRequired += result.approvalRequiredCount();
            delayed += result.delayedCount();
            deviated += result.deviatedCount();
        }

        auditLogService.record(AuditAction.SIMULATION_CREATED, "simulation_batch", batchId,
                null, "CREATED", Map.of("requestedCount", count, "processedCount", processed));
        return new SimulationResult(count, processed, duplicate, failed, outboxCreated, approvalRequired, delayed, deviated);
    }

    private NexusLogisticsEventRequest request(String batchId, int index) {
        String seed = batchId + ":" + index;
        String factoryId = FACTORIES[hash.bounded(seed + ":factory", FACTORIES.length)];
        String destinationCode = DESTINATIONS[hash.bounded(seed + ":destination", DESTINATIONS.length)];
        String priority = PRIORITIES[hash.bounded(seed + ":priority", PRIORITIES.length)];
        String eventType = EVENT_TYPES[hash.bounded(seed + ":type", EVENT_TYPES.length)];
        boolean coldChain = hash.bounded(seed + ":cold", 10) == 0;
        String day = LocalDate.now(clock).format(DATE);
        String shortHash = hash.shortHash(seed);
        String shipmentId = "SHIP-SIM-" + day + "-" + String.format("%06d", index + 1);
        String eventId = "evt-nexus-sim-" + day + "-" + shortHash;
        String idempotencyKey = "NEXUS:" + eventType + ":" + factoryId + ":" + shipmentId + ":" + shortHash;
        Instant occurredAt = clock.instant().minusSeconds(index);
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
                        10 + hash.bounded(seed + ":quantity", 490),
                        coldChain
                )
        );
    }
}
