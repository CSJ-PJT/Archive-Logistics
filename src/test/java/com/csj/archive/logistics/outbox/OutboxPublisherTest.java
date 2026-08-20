package com.csj.archive.logistics.outbox;

import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.ledger.LedgerPublishProperties;
import com.csj.archive.logistics.ledger.LedgerPublisherClient;
import com.csj.archive.logistics.economy.LogisticsEconomyService;
import com.csj.archive.logistics.ledger.LedgerBulkPublishResponse;
import com.csj.archive.logistics.runtime.ArchiveOsRouteOutboxProjectionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.eq;

class OutboxPublisherTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LogisticsOutboxRepository outboxRepository = mock(LogisticsOutboxRepository.class);
    private final LedgerPublishAttemptRepository attemptRepository = mock(LedgerPublishAttemptRepository.class);
    private final LedgerPublisherClient ledgerPublisherClient = mock(LedgerPublisherClient.class);
    private final LedgerPublishProperties ledgerProperties = new LedgerPublishProperties();
    private final OutboxProperties outboxProperties = new OutboxProperties();
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final IdGenerator idGenerator = new IdGenerator(Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC),
            new com.csj.archive.logistics.common.DeterministicHash());
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);
    private final LogisticsEconomyService economyService = mock(LogisticsEconomyService.class);

    @Test
    void hopExceededEventsAreSkippedWithoutLedgerPublish() {
        ledgerProperties.setEnabled(true);
        OutboxPublisher publisher = new OutboxPublisher(
                outboxRepository,
                attemptRepository,
                ledgerPublisherClient,
                ledgerProperties,
                outboxProperties,
                auditLogService,
                idGenerator,
                clock,
                economyService,
                null
        );
        LogisticsOutboxEntity blocked = outbox("evt-hop-block", "ROUTE-HOP-BLOCK", 6, 3);
        when(outboxRepository.findPublishable(any(Collection.class), any(), any(Pageable.class)))
                .thenReturn(List.of(blocked));

        OutboxPublishResult result = publisher.publishAvailable("test", 50);

        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.publishedCount()).isEqualTo(0);
        assertThat(blocked.status()).isEqualTo(OutboxStatus.SKIPPED);
        verify(ledgerPublisherClient, never()).publish(any());
        verify(attemptRepository).save(any(LedgerPublishAttemptEntity.class));
    }

    @Test
    void ledgerPublishedProjectionIsCreatedOnlyAfterPublishedStateIsSavedIncludingDuplicateSuccess() {
        ledgerProperties.setEnabled(true);
        ArchiveOsRouteOutboxProjectionService projection = mock(ArchiveOsRouteOutboxProjectionService.class);
        OutboxPublisher publisher = new OutboxPublisher(outboxRepository, attemptRepository, ledgerPublisherClient, ledgerProperties,
                outboxProperties, auditLogService, idGenerator, clock, economyService, projection);
        LogisticsOutboxEntity accepted = outbox("evt-accepted", "ROUTE-A", 0, 5);
        LogisticsOutboxEntity duplicate = outbox("evt-duplicate", "ROUTE-B", 0, 5);
        when(outboxRepository.findPublishable(any(Collection.class), any(), any(Pageable.class))).thenReturn(List.of(accepted, duplicate));
        when(ledgerPublisherClient.publish(any())).thenReturn(new LedgerBulkPublishResponse(2, 1, 1, 0, List.of()));

        publisher.publishAvailable("test", 50);

        assertThat(accepted.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(duplicate.status()).isEqualTo(OutboxStatus.PUBLISHED);
        var order = inOrder(outboxRepository, projection);
        order.verify(outboxRepository).saveAll(any());
        order.verify(projection).ledgerPublished(eq(accepted), any());
        order.verify(projection).ledgerPublished(eq(duplicate), any());
    }

    @Test
    void ledgerProjectionIsNotCreatedForRetryOrFailedResponses() {
        ledgerProperties.setEnabled(true);
        ArchiveOsRouteOutboxProjectionService projection = mock(ArchiveOsRouteOutboxProjectionService.class);
        OutboxPublisher publisher = new OutboxPublisher(outboxRepository, attemptRepository, ledgerPublisherClient, ledgerProperties,
                outboxProperties, auditLogService, idGenerator, clock, economyService, projection);
        LogisticsOutboxEntity event = outbox("evt-failed", "ROUTE-F", 0, 5);
        when(outboxRepository.findPublishable(any(Collection.class), any(), any(Pageable.class))).thenReturn(List.of(event));
        when(ledgerPublisherClient.publish(any())).thenReturn(new LedgerBulkPublishResponse(1, 0, 0, 1,
                List.of(new LedgerBulkPublishResponse.EventResult(event.eventId(), "FAILED", null, "500"))));

        publisher.publishAvailable("test", 50);

        assertThat(event.status()).isIn(OutboxStatus.RETRY, OutboxStatus.FAILED);
        verify(projection, never()).ledgerPublished(any(), any());
    }

    @Test
    void archiveOsSnapshotFailureDoesNotUndoLedgerPublished() {
        ledgerProperties.setEnabled(true);
        ArchiveOsRouteOutboxProjectionService projection = mock(ArchiveOsRouteOutboxProjectionService.class);
        OutboxPublisher publisher = new OutboxPublisher(outboxRepository, attemptRepository, ledgerPublisherClient, ledgerProperties,
                outboxProperties, auditLogService, idGenerator, clock, economyService, projection);
        LogisticsOutboxEntity event = outbox("evt-snapshot-failure", "ROUTE-S", 0, 5);
        when(outboxRepository.findPublishable(any(Collection.class), any(), any(Pageable.class))).thenReturn(List.of(event));
        when(ledgerPublisherClient.publish(any())).thenReturn(LedgerBulkPublishResponse.success(1));
        org.mockito.Mockito.doThrow(new RuntimeException("ArchiveOS down")).when(projection).ledgerPublished(any(), any());

        publisher.publishAvailable("test", 50);

        assertThat(event.status()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    private LogisticsOutboxEntity outbox(String eventId, String routePlanId, int hopCount, int maxHop) {
        JsonNode payload = objectMapper.createObjectNode()
                .put("eventId", eventId)
                .put("routePlanId", routePlanId)
                .put("hopCount", hopCount)
                .put("maxHop", maxHop);

        return new LogisticsOutboxEntity(
                new LogisticsOutboxEvent(
                        eventId,
                        "LOGITICS:LOGISTICS_COST_CONFIRMED:" + routePlanId,
                        "Archive-Logitics",
                        "LOGISTICS_COST_CONFIRMED",
                        "ROUTE_PLAN",
                        routePlanId,
                        payload
                ),
                java.time.LocalDateTime.of(2026, 1, 15, 0, 0)
        );
    }

}
