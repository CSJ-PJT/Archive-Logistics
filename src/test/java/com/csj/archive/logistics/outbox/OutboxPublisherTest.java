package com.csj.archive.logistics.outbox;

import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.ledger.LedgerPublishProperties;
import com.csj.archive.logistics.ledger.LedgerPublisherClient;
import com.csj.archive.logistics.economy.LogisticsEconomyService;
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
                economyService
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
