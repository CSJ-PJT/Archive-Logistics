package com.csj.archive.logistics.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class OutboxServiceRetryTest {
    @Test
    void retryFailedUsesBoundedPageInsteadOfLoadingTheWholeHistory() {
        LogisticsOutboxRepository repository = mock(LogisticsOutboxRepository.class);
        OutboxPublisher publisher = mock(OutboxPublisher.class);
        OutboxProperties properties = new OutboxProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T09:00:00Z"), ZoneOffset.UTC);
        LogisticsOutboxEntity failed = mock(LogisticsOutboxEntity.class);
        PageRequest page = PageRequest.of(0, 250);
        when(repository.findByStatus(OutboxStatus.FAILED, page))
                .thenReturn(new PageImpl<>(List.of(failed), page, 1));

        OutboxService service = new OutboxService(repository, publisher, properties, clock);

        assertThat(service.retryFailed(250)).isEqualTo(1);
        verify(failed).resetForRetry(java.time.LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        verify(repository).saveAll(List.of(failed));
    }

    @Test
    void retryFailedClampsOperatorLimit() {
        LogisticsOutboxRepository repository = mock(LogisticsOutboxRepository.class);
        PageRequest page = PageRequest.of(0, 1_000);
        when(repository.findByStatus(OutboxStatus.FAILED, page))
                .thenReturn(new PageImpl<>(List.of(), page, 0));
        OutboxService service = new OutboxService(repository, mock(OutboxPublisher.class),
                new OutboxProperties(), Clock.systemUTC());

        assertThat(service.retryFailed(50_000)).isZero();
        verify(repository).findByStatus(OutboxStatus.FAILED, page);
    }
}
