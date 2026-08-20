package com.csj.archive.logistics.outbox;

import com.csj.archive.logistics.common.NotFoundException;
import com.csj.archive.logistics.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxService {
    private static final int MAX_SCOPED_EVENTS = 50;
    private final LogisticsOutboxRepository outboxRepository;
    private final OutboxPublisher outboxPublisher;
    private final OutboxProperties outboxProperties;
    private final Clock clock;

    public OutboxService(LogisticsOutboxRepository outboxRepository,
                         OutboxPublisher outboxPublisher,
                         OutboxProperties outboxProperties,
                         Clock clock) {
        this.outboxRepository = outboxRepository;
        this.outboxPublisher = outboxPublisher;
        this.outboxProperties = outboxProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<OutboxEventResponse> events(OutboxStatus status, Pageable pageable) {
        var page = status == null ? outboxRepository.findAll(pageable) : outboxRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(OutboxEventResponse::from));
    }

    @Transactional(readOnly = true)
    public OutboxEventResponse event(String eventId) {
        return outboxRepository.findByEventId(eventId)
                .map(OutboxEventResponse::from)
                .orElseThrow(() -> new NotFoundException("Outbox event not found: " + eventId));
    }

    @Transactional(readOnly = true)
    public OutboxSummaryResponse summary() {
        return new OutboxSummaryResponse(
                outboxRepository.countByStatus(OutboxStatus.PENDING),
                outboxRepository.countByStatus(OutboxStatus.PUBLISHED),
                outboxRepository.countByStatus(OutboxStatus.FAILED),
                outboxRepository.countByStatus(OutboxStatus.RETRY),
                outboxRepository.countByStatus(OutboxStatus.SKIPPED)
        );
    }

    public OutboxPublishResult publish() {
        return outboxPublisher.publishAvailable("api", outboxProperties.getChunkSize());
    }

    @Transactional(readOnly = true)
    public OutboxScopedPublishResponse preview(String correlationId) {
        return response(correlationId, scoped(correlationId), "PREVIEW");
    }

    @Transactional
    public OutboxScopedPublishResponse publishEvent(String eventId) {
        LogisticsOutboxEntity event = outboxRepository.findByEventId(eventId)
                .orElseThrow(() -> new NotFoundException("Outbox event not found: " + eventId));
        String correlationId = correlationId(event);
        if (event.status() == OutboxStatus.PUBLISHED) {
            return response(correlationId, List.of(event), "ALREADY_PUBLISHED");
        }
        if (event.status() == OutboxStatus.FAILED) {
            return response(correlationId, List.of(event), "FAILED_REQUIRES_RETRY_POLICY");
        }
        if (event.status() == OutboxStatus.SKIPPED) {
            return response(correlationId, List.of(event), "SKIPPED_TERMINAL");
        }
        if (!isPublishable(event, LocalDateTime.now(clock))) {
            return response(correlationId, List.of(event), "RETRY_NOT_DUE");
        }
        outboxPublisher.publishSelected(List.of(event), "event:" + eventId);
        return response(correlationId, List.of(event), "PUBLISHED".equals(event.status().name()) ? "NONE" : "DELIVERY_RESULT_RECORDED");
    }

    @Transactional
    public int retryFailed() {
        LocalDateTime now = LocalDateTime.now(clock);
        var failed = outboxRepository.findByStatus(OutboxStatus.FAILED);
        failed.forEach(event -> event.resetForRetry(now));
        outboxRepository.saveAll(failed);
        return failed.size();
    }

    private List<LogisticsOutboxEntity> scoped(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId is required");
        }
        return outboxRepository.findByCorrelationId(correlationId.trim(), MAX_SCOPED_EVENTS);
    }

    private OutboxScopedPublishResponse response(String correlationId, List<LogisticsOutboxEntity> events, String requestedBlocker) {
        LocalDateTime now = LocalDateTime.now(clock);
        int publishable = (int) events.stream().filter(event -> isPublishable(event, now)).count();
        int skipped = (int) events.stream().filter(event -> event.status() == OutboxStatus.SKIPPED).count();
        int published = (int) events.stream().filter(event -> event.status() == OutboxStatus.PUBLISHED).count();
        int failed = (int) events.stream().filter(event -> event.status() == OutboxStatus.FAILED).count();
        String blocker = events.isEmpty() ? "NO_MATCHING_EVENTS" : requestedBlocker;
        return new OutboxScopedPublishResponse(correlationId, events.size(), publishable, skipped, published, failed,
                events.stream().map(LogisticsOutboxEntity::eventId).toList(), blocker);
    }

    private boolean isPublishable(LogisticsOutboxEntity event, LocalDateTime now) {
        return (event.status() == OutboxStatus.PENDING || event.status() == OutboxStatus.RETRY)
                && (event.nextRetryAt() == null || !event.nextRetryAt().isAfter(now));
    }

    private String correlationId(LogisticsOutboxEntity event) {
        String value = event.payload() == null ? null : event.payload().path("correlationId").asText(null);
        return value == null || value.isBlank() ? "UNSCOPED" : value;
    }
}
