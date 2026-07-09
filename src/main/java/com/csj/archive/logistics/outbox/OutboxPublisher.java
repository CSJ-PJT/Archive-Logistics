package com.csj.archive.logistics.outbox;

import com.csj.archive.logistics.audit.AuditAction;
import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.economy.LogisticsEconomyService;
import com.csj.archive.logistics.ledger.LedgerBulkPublishResponse;
import com.csj.archive.logistics.ledger.LedgerCompatibleEventPayload;
import com.csj.archive.logistics.ledger.LedgerPublishProperties;
import com.csj.archive.logistics.ledger.LedgerPublisherClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OutboxPublisher {
    private static final Collection<OutboxStatus> PUBLISHABLE = List.of(OutboxStatus.PENDING, OutboxStatus.RETRY);

    private final LogisticsOutboxRepository outboxRepository;
    private final LedgerPublishAttemptRepository attemptRepository;
    private final LedgerPublisherClient ledgerPublisherClient;
    private final LedgerPublishProperties ledgerProperties;
    private final OutboxProperties outboxProperties;
    private final AuditLogService auditLogService;
    private final com.csj.archive.logistics.common.IdGenerator idGenerator;
    private final Clock clock;
    private final LogisticsEconomyService economyService;

    public OutboxPublisher(LogisticsOutboxRepository outboxRepository,
                           LedgerPublishAttemptRepository attemptRepository,
                           LedgerPublisherClient ledgerPublisherClient,
                           LedgerPublishProperties ledgerProperties,
                           OutboxProperties outboxProperties,
                           AuditLogService auditLogService,
                           com.csj.archive.logistics.common.IdGenerator idGenerator,
                           Clock clock,
                           LogisticsEconomyService economyService) {
        this.outboxRepository = outboxRepository;
        this.attemptRepository = attemptRepository;
        this.ledgerPublisherClient = ledgerPublisherClient;
        this.ledgerProperties = ledgerProperties;
        this.outboxProperties = outboxProperties;
        this.auditLogService = auditLogService;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.economyService = economyService;
    }

    @Transactional
    public OutboxPublishResult publishAvailable(String trigger, int chunkSize) {
        LocalDateTime startedAt = LocalDateTime.now(clock);
        String batchId = idGenerator.batchId("outbox-" + trigger);
        List<LogisticsOutboxEntity> events = outboxRepository.findPublishable(
                PUBLISHABLE,
                startedAt,
                PageRequest.of(0, Math.max(1, chunkSize))
        );

        if (events.isEmpty()) {
            recordAttempt(batchId, 0, 0, 0, LedgerPublishResultStatus.SKIPPED, null, startedAt, LocalDateTime.now(clock));
            return new OutboxPublishResult(
                    batchId,
                    ledgerProperties.isEnabled(),
                    false,
                    ledgerProperties.endpoint(),
                    ledgerProperties.getContractMode().name(),
                    0,
                    0,
                    0,
                    0,
                    0,
                    LedgerPublishResultStatus.SKIPPED.name(),
                    "No publishable outbox events."
            );
        }

        if (!ledgerProperties.isEnabled()) {
            recordAttempt(batchId, events.size(), 0, 0, LedgerPublishResultStatus.DRY_RUN, null, startedAt, LocalDateTime.now(clock));
            auditLogService.record(
                    AuditAction.OUTBOX_PUBLISH_SKIPPED,
                    "outbox_batch",
                    batchId,
                    null,
                    "DRY_RUN",
                    Map.of("eventCount", events.size(), "ledgerEnabled", false)
            );
            return new OutboxPublishResult(
                    batchId,
                    false,
                    true,
                    ledgerProperties.endpoint(),
                    ledgerProperties.getContractMode().name(),
                    events.size(),
                    0,
                    0,
                    0,
                    events.size(),
                    LedgerPublishResultStatus.DRY_RUN.name(),
                    "Ledger disabled; outbox state was not changed."
            );
        }

        int skipped = 0;
        List<LogisticsOutboxEntity> publishable = new ArrayList<>();
        for (LogisticsOutboxEntity event : events) {
            if (!isHopAllowed(event)) {
                event.markSkipped("hopCount exceeds maxHop", startedAt);
                skipped++;
                auditLogService.record(
                        AuditAction.OUTBOX_PUBLISH_SKIPPED,
                        "outbox_event",
                        event.eventId(),
                        null,
                        "SKIPPED",
                        Map.of(
                                "batchId", batchId,
                                "reason", "hopCount exceeds maxHop",
                                "hopCount", hopCount(event),
                                "maxHop", maxHop(event)
                        )
                );
            } else {
                publishable.add(event);
            }
        }

        if (publishable.isEmpty()) {
            outboxRepository.saveAll(events);
            recordAttempt(batchId, events.size(), 0, 0, LedgerPublishResultStatus.SKIPPED, "All events skipped by hop limit.",
                    startedAt, LocalDateTime.now(clock));
            return new OutboxPublishResult(
                    batchId,
                    true,
                    false,
                    ledgerProperties.endpoint(),
                    ledgerProperties.getContractMode().name(),
                    events.size(),
                    0,
                    0,
                    0,
                    skipped,
                    LedgerPublishResultStatus.SKIPPED.name(),
                    "Publish blocked by hop limit."
            );
        }

        try {
            List<LedgerCompatibleEventPayload> payloads = publishable.stream()
                    .map(LogisticsOutboxEntity::toLedgerPayload)
                    .toList();
            LedgerBulkPublishResponse response = ledgerPublisherClient.publish(payloads);
            LocalDateTime completedAt = LocalDateTime.now(clock);

            Map<String, LedgerBulkPublishResponse.EventResult> resultByEventId = response.results() == null
                    ? Map.of()
                    : response.results().stream().collect(Collectors.toMap(
                    LedgerBulkPublishResponse.EventResult::eventId,
                    Function.identity(),
                    (left, right) -> left
            ));

            boolean allSuccessful = response.failed() == 0 && resultByEventId.isEmpty();
            int published = 0;
            int failed = 0;
            int retried = 0;

            for (LogisticsOutboxEntity event : publishable) {
                LedgerBulkPublishResponse.EventResult eventResult = resultByEventId.get(event.eventId());
                if (allSuccessful || (eventResult != null && eventResult.successful())) {
                    event.markPublished(completedAt);
                    auditLogService.record(
                            AuditAction.OUTBOX_PUBLISH_SUCCEEDED,
                            "outbox_event",
                            event.eventId(),
                            null,
                            OutboxStatus.PUBLISHED.name(),
                            Map.of("batchId", batchId)
                    );
                    published++;
                    economyService.recordLedgerPublishFeeEvents(event);
                } else {
                    String message = eventResult == null || eventResult.message() == null || eventResult.message().isBlank()
                            ? "Ledger bulk publish failed without per-event detail"
                            : eventResult.message();
                    event.scheduleRetry(message, outboxProperties.getMaxRetryCount(), completedAt);
                    auditLogService.record(
                            AuditAction.OUTBOX_RETRY_SCHEDULED,
                            "outbox_event",
                            event.eventId(),
                            null,
                            event.status().name(),
                            Map.of("batchId", batchId, "error", message)
                    );
                    if (event.status() == OutboxStatus.FAILED) {
                        failed++;
                    } else {
                        retried++;
                    }
                }
            }

            outboxRepository.saveAll(events);
            LedgerPublishResultStatus status = failed == 0 && retried == 0
                    ? LedgerPublishResultStatus.SUCCESS
                    : LedgerPublishResultStatus.PARTIAL_FAILURE;
            recordAttempt(batchId, events.size(), published, failed + retried, status, null, startedAt, completedAt);
            return new OutboxPublishResult(
                    batchId,
                    true,
                    false,
                    ledgerProperties.endpoint(),
                    ledgerProperties.getContractMode().name(),
                    events.size(),
                    published,
                    failed,
                    retried,
                    skipped,
                    status.name(),
                    "Publish completed."
            );
        } catch (RuntimeException exception) {
            LocalDateTime completedAt = LocalDateTime.now(clock);
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            int failed = 0;
            int retried = 0;

            for (LogisticsOutboxEntity event : publishable) {
                event.scheduleRetry(message, outboxProperties.getMaxRetryCount(), completedAt);
                auditLogService.record(
                        AuditAction.OUTBOX_PUBLISH_FAILED,
                        "outbox_event",
                        event.eventId(),
                        null,
                        event.status().name(),
                        Map.of("batchId", batchId, "error", message)
                );
                if (event.status() == OutboxStatus.FAILED) {
                    failed++;
                } else {
                    retried++;
                }
            }

            outboxRepository.saveAll(events);
            recordAttempt(batchId, events.size(), 0, failed + retried, LedgerPublishResultStatus.FAILED,
                    message, startedAt, completedAt);
            return new OutboxPublishResult(
                    batchId,
                    true,
                    false,
                    ledgerProperties.endpoint(),
                    ledgerProperties.getContractMode().name(),
                    events.size(),
                    0,
                    failed,
                    retried,
                    skipped,
                    LedgerPublishResultStatus.FAILED.name(),
                    message
            );
        }
    }

    @Scheduled(fixedDelayString = "${archive.outbox.scheduler.fixed-delay-ms:${archive.outbox.scheduler.interval-ms:30000}}")
    public void publishOnSchedule() {
        if (outboxProperties.getScheduler().isEnabled()) {
            publishAvailable("scheduler", outboxProperties.getChunkSize());
        }
    }

    private void recordAttempt(String batchId, int eventCount, int successCount, int failureCount,
                               LedgerPublishResultStatus resultStatus, String errorMessage,
                               LocalDateTime startedAt, LocalDateTime completedAt) {
        attemptRepository.save(new LedgerPublishAttemptEntity(
                batchId,
                eventCount,
                successCount,
                failureCount,
                ledgerProperties.isEnabled(),
                ledgerProperties.endpoint(),
                ledgerProperties.getContractMode(),
                resultStatus,
                trim(errorMessage),
                startedAt,
                completedAt
        ));
    }

    private boolean isHopAllowed(LogisticsOutboxEntity event) {
        if (event.payload() == null) {
            return true;
        }
        return hopCount(event) <= maxHop(event);
    }

    private int hopCount(LogisticsOutboxEntity event) {
        return event.payload().path("hopCount").asInt(0);
    }

    private int maxHop(LogisticsOutboxEntity event) {
        return event.payload().path("maxHop").asInt(economyService.maxHop());
    }

    private String trim(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
