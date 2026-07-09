package com.csj.archive.logistics.outbox;

import com.csj.archive.logistics.common.NotFoundException;
import com.csj.archive.logistics.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class OutboxService {
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

    @Transactional
    public int retryFailed() {
        LocalDateTime now = LocalDateTime.now(clock);
        var failed = outboxRepository.findByStatus(OutboxStatus.FAILED);
        failed.forEach(event -> event.resetForRetry(now));
        outboxRepository.saveAll(failed);
        return failed.size();
    }
}
