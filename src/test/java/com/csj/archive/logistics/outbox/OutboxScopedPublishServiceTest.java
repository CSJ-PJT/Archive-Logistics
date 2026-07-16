package com.csj.archive.logistics.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutboxScopedPublishServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC);
    private final LogisticsOutboxRepository repository = mock(LogisticsOutboxRepository.class);
    private final OutboxPublisher publisher = mock(OutboxPublisher.class);
    private final OutboxService service = new OutboxService(repository, publisher, new OutboxProperties(), clock);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void previewIsCorrelationBoundedAndDoesNotSelectOtherRows() {
        LogisticsOutboxEntity selected = event("evt-selected", "CORR-selected");
        when(repository.findByCorrelationId("CORR-selected", 50)).thenReturn(List.of(selected));

        OutboxScopedPublishResponse preview = service.preview("CORR-selected");

        assertThat(preview.correlationId()).isEqualTo("CORR-selected");
        assertThat(preview.selected()).isEqualTo(1);
        assertThat(preview.publishable()).isEqualTo(1);
        assertThat(preview.eventIds()).containsExactly("evt-selected");
        verify(repository, never()).findAll();
    }

    @Test
    void singleEventPublishNeverExpandsToTheLegacyBacklog() {
        LogisticsOutboxEntity selected = event("evt-single", "CORR-single");
        when(repository.findByEventId("evt-single")).thenReturn(java.util.Optional.of(selected));

        OutboxScopedPublishResponse result = service.publishEvent("evt-single");

        assertThat(result.selected()).isEqualTo(1);
        assertThat(result.eventIds()).containsExactly("evt-single");
        verify(publisher).publishSelected(anyList(), org.mockito.ArgumentMatchers.eq("event:evt-single"));
        verify(repository, never()).findPublishable(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishedAndFailedEventsDoNotBypassTheirExistingPolicies() {
        LogisticsOutboxEntity published = event("evt-published", "CORR-terminal");
        published.markPublished(LocalDateTime.now(clock));
        when(repository.findByEventId("evt-published")).thenReturn(java.util.Optional.of(published));

        OutboxScopedPublishResponse duplicate = service.publishEvent("evt-published");

        assertThat(duplicate.blocker()).isEqualTo("ALREADY_PUBLISHED");
        verify(publisher, never()).publishSelected(anyList(), org.mockito.ArgumentMatchers.anyString());

        LogisticsOutboxEntity failed = event("evt-failed", "CORR-terminal");
        failed.scheduleRetry("terminal", 1, LocalDateTime.now(clock));
        when(repository.findByEventId("evt-failed")).thenReturn(java.util.Optional.of(failed));

        OutboxScopedPublishResponse blocked = service.publishEvent("evt-failed");

        assertThat(blocked.blocker()).isEqualTo("FAILED_REQUIRES_RETRY_POLICY");
        verify(publisher, never()).publishSelected(anyList(), org.mockito.ArgumentMatchers.anyString());
    }

    private LogisticsOutboxEntity event(String eventId, String correlationId) {
        return new LogisticsOutboxEntity(new LogisticsOutboxEvent(
                eventId,
                "LOGISTICS:" + eventId,
                "archive-logistics",
                "LOGISTICS_COST_CONFIRMED",
                "ROUTE_PLAN",
                "route-" + eventId,
                mapper.createObjectNode().put("eventId", eventId).put("correlationId", correlationId)
                        .put("hopCount", 0).put("maxHop", 5)), LocalDateTime.now(clock));
    }
}
