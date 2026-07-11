package com.csj.archive.logistics.runtime;

import com.csj.archive.logistics.common.DeterministicHash;
import com.csj.archive.logistics.event.BulkEventProcessingResult;
import com.csj.archive.logistics.event.NexusLogisticsBulkEventRequest;
import com.csj.archive.logistics.event.NexusLogisticsEventService;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.csj.archive.logistics.workforce.WorkforceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeWorkLoopTest {
    private final RuntimeWorkLoopProperties properties = new RuntimeWorkLoopProperties();
    private final NexusLogisticsEventService nexusService = mock(NexusLogisticsEventService.class);
    private final WorkforceService workforceService = mock(WorkforceService.class);
    private final LogisticsOutboxRepository outboxRepository = mock(LogisticsOutboxRepository.class);
    private final RoutePlanRepository routePlanRepository = mock(RoutePlanRepository.class);
    private final RuntimeEventService runtimeEventService = mock(RuntimeEventService.class);
    private final ShipmentLifecycleService shipmentLifecycleService = mock(ShipmentLifecycleService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC);
    private final RuntimeWorkLoop loop = new RuntimeWorkLoop(
            properties,
            nexusService,
            workforceService,
            outboxRepository,
            routePlanRepository,
            runtimeEventService,
            shipmentLifecycleService,
            new DeterministicHash(),
            clock
    );

    @Test
    void autoRunTickCreatesLimitedSyntheticWork() {
        properties.getAutorun().setEnabled(true);
        properties.setMaxEventsPerTick(3);
        when(outboxRepository.countByStatusIn(anyCollection())).thenReturn(0L);
        when(routePlanRepository.latestCreatedAt()).thenReturn(LocalDateTime.parse("2026-07-10T10:00:00"));
        when(nexusService.processBulk(any())).thenReturn(new BulkEventProcessingResult(3, 3, 0, 0, 3, 0, 0, 0));

        RuntimeWorkLoop.RuntimeTickResult result = loop.runTick();

        ArgumentCaptor<NexusLogisticsBulkEventRequest> captor = ArgumentCaptor.forClass(NexusLogisticsBulkEventRequest.class);
        verify(nexusService).processBulk(captor.capture());
        verify(workforceService).runWorkday(LocalDate.parse("2026-07-10"));
        verify(shipmentLifecycleService).advance(any(), any());
        assertThat(captor.getValue().events()).hasSize(3);
        assertThat(result.eventsProduced()).isEqualTo(3);
        assertThat(loop.status().lastWorkAt()).isEqualTo(LocalDateTime.parse("2026-07-10T10:00:00"));
    }

    @Test
    void tickDoesNotExceedBacklogGuard() {
        properties.getAutorun().setEnabled(true);
        properties.setMaxEventsPerTick(10);
        properties.setMaxBacklogPerTick(5);
        when(outboxRepository.countByStatusIn(anyCollection())).thenReturn(5L);

        RuntimeWorkLoop.RuntimeTickResult result = loop.runTick();

        verify(nexusService, times(0)).processBulk(any());
        verify(workforceService).runWorkday(LocalDate.parse("2026-07-10"));
        assertThat(result.eventsProduced()).isZero();
        assertThat(result.schedulerStatus()).isEqualTo("BACKLOG_LIMITED");
    }

    @Test
    void duplicateTickUsesSameIdempotentEvents() {
        properties.getAutorun().setEnabled(true);
        properties.setMaxEventsPerTick(2);
        when(outboxRepository.countByStatusIn(anyCollection())).thenReturn(0L);
        when(nexusService.processBulk(any())).thenReturn(new BulkEventProcessingResult(2, 2, 0, 0, 2, 0, 0, 0));

        loop.runTick();
        loop.runTick();

        ArgumentCaptor<NexusLogisticsBulkEventRequest> captor = ArgumentCaptor.forClass(NexusLogisticsBulkEventRequest.class);
        verify(nexusService, times(2)).processBulk(captor.capture());
        assertThat(captor.getAllValues().get(0).events().getFirst().eventId())
                .isEqualTo(captor.getAllValues().get(1).events().getFirst().eventId());
        assertThat(captor.getAllValues().get(0).events().getFirst().idempotencyKey())
                .isEqualTo(captor.getAllValues().get(1).events().getFirst().idempotencyKey());
    }

    @Test
    void statusReturnsRuntimeFields() {
        properties.getAutorun().setEnabled(true);
        when(outboxRepository.countByStatusIn(any(Collection.class))).thenReturn(2L);
        when(routePlanRepository.latestCreatedAt()).thenReturn(LocalDateTime.parse("2026-07-10T09:59:00"));

        RuntimeStatusResponse status = loop.status();

        assertThat(status.service()).isEqualTo("Archive-Logistics");
        assertThat(status.runtimeActive()).isTrue();
        assertThat(status.autoRunEnabled()).isTrue();
        assertThat(status.lastEventAt()).isEqualTo(LocalDateTime.parse("2026-07-10T09:59:00"));
        assertThat(status.backlogCount()).isEqualTo(2);
    }
}
