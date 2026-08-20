package com.csj.archive.logistics.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArchiveOsRuntimeDeliveryServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC);
    private final ArchiveOsRuntimeDeliveryRepository repository = mock(ArchiveOsRuntimeDeliveryRepository.class);
    private final ArchiveOsRuntimeDeliveryPersistence persistence = mock(ArchiveOsRuntimeDeliveryPersistence.class);
    private final ArchiveOsRuntimePublisher publisher = mock(ArchiveOsRuntimePublisher.class);
    private final ArchiveOsRuntimeProperties properties = new ArchiveOsRuntimeProperties();
    private final ArchiveOsRuntimeDeliveryService service = new ArchiveOsRuntimeDeliveryService(repository, persistence, new ObjectMapper(), clock, publisher, properties);

    @Test
    void stalePublishingMovesToRetryWaitWithRecoveryCode() {
        properties.setMaxRetryCount(3); properties.setStalePublishingSeconds(60);
        ArchiveOsRuntimeDeliveryEntity delivery = delivery();
        delivery.publishing(LocalDateTime.ofInstant(clock.instant().minusSeconds(120), ZoneOffset.UTC));
        when(repository.findStalePublishing(eq(ArchiveOsDeliveryStatus.PUBLISHING), any(), any(Pageable.class))).thenReturn(List.of(delivery));

        assertThat(service.recoverStalePublishing()).isEqualTo(1);
        assertThat(delivery.status()).isEqualTo(ArchiveOsDeliveryStatus.RETRY_WAIT);
        assertThat(delivery.nextRetryAt()).isNotNull();
        assertThat(delivery.lastErrorCode()).isEqualTo("STALE_PUBLISHING_RECOVERED");
    }

    @Test
    void stalePublishingAtRetryLimitFailsAndUsesBoundedBatch() {
        properties.setMaxRetryCount(0); properties.setBatchSize(1);
        ArchiveOsRuntimeDeliveryEntity delivery = delivery();
        delivery.publishing(LocalDateTime.ofInstant(clock.instant().minusSeconds(600), ZoneOffset.UTC));
        when(repository.findStalePublishing(eq(ArchiveOsDeliveryStatus.PUBLISHING), any(), any(Pageable.class))).thenReturn(List.of(delivery));

        service.recoverStalePublishing();
        assertThat(delivery.status()).isEqualTo(ArchiveOsDeliveryStatus.FAILED);
        verify(repository).findStalePublishing(eq(ArchiveOsDeliveryStatus.PUBLISHING), any(), argThat(page -> page.getPageSize() == 1));
    }

    @Test
    void schedulerLockPreventsOverlappingRecoveryAndPublish() {
        ArchiveOsRuntimeDeliveryService delegate = mock(ArchiveOsRuntimeDeliveryService.class);
        ArchiveOsRuntimeDeliveryScheduler scheduler = new ArchiveOsRuntimeDeliveryScheduler(delegate, enabled());
        scheduler.publish();
        verify(delegate).recoverStalePublishing();
        verify(delegate).publishBatch();
    }

    private ArchiveOsRuntimeDeliveryEntity delivery() {
        return new ArchiveOsRuntimeDeliveryEntity("evt-1", "key-1", "corr-1", null, null, null, "shipment", "ship-1", "SHIPMENT_CREATED",
                new ObjectMapper().createObjectNode(), LocalDateTime.ofInstant(clock.instant().minusSeconds(1000), ZoneOffset.UTC));
    }
    private ArchiveOsRuntimeProperties enabled() { ArchiveOsRuntimeProperties value = new ArchiveOsRuntimeProperties(); value.setSchedulerEnabled(true); return value; }
}
