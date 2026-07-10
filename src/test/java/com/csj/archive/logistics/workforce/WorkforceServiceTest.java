package com.csj.archive.logistics.workforce;

import com.csj.archive.logistics.common.DeterministicHash;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.outbox.OutboxStatus;
import com.csj.archive.logistics.route.RoutePlanRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkforceServiceTest {
    private final WorkforceAllocationRepository allocationRepository = mock(WorkforceAllocationRepository.class);
    private final WorkdayProductivityRepository productivityRepository = mock(WorkdayProductivityRepository.class);
    private final RoutePlanRepository routePlanRepository = mock(RoutePlanRepository.class);
    private final LogisticsOutboxRepository outboxRepository = mock(LogisticsOutboxRepository.class);
    private final WorkforceProperties properties = new WorkforceProperties();
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-10T00:00:00Z"), ZoneOffset.UTC);
    private final IdGenerator idGenerator = new IdGenerator(clock, new DeterministicHash());

    @Test
    void disabledWorkforceUsesBaselineCapacity() {
        WorkforceService service = service();
        when(productivityRepository.findTopByOrderByWorkDateDescCreatedAtDesc()).thenReturn(Optional.empty());
        when(routePlanRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(10L);
        when(routePlanRepository.countByDelayedTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(2L);
        when(outboxRepository.countByStatusIn(any(Collection.class))).thenReturn(5L);

        WorkforceSummaryResponse summary = service.workforceSummary();

        assertThat(summary.workforceEnabled()).isFalse();
        assertThat(summary.baselineCapacity()).isTrue();
        assertThat(summary.capacityEvents()).isEqualTo(210L);
        assertThat(summary.workloadEvents()).isEqualTo(17L);
        assertThat(summary.backlogEvents()).isZero();
        assertThat(summary.status()).isEqualTo("PRODUCTIVITY_REPORTED");
    }

    @Test
    void enabledWorkforceAllocationCanDetectCapacityShortage() {
        properties.setEnabled(true);
        properties.setDispatcherDailyCapacity(1);
        properties.setDriverDailyCapacity(1);
        properties.setDelayResponderDailyCapacity(1);
        WorkforceService service = service();
        LocalDate date = LocalDate.parse("2026-07-10");
        WorkforceAllocationEntity allocation = new WorkforceAllocationEntity(
                "WF-ALLOC-20260710-TEST",
                new WorkforceAllocationRequest(
                        "ArchiveOS",
                        date,
                        "WORKDAY-20260710",
                        1,
                        1,
                        0,
                        "SIM-1",
                        "CYCLE-1",
                        "CORR-1",
                        "CAUSE-1",
                        0,
                        5,
                        "test allocation"
                ),
                400_000L,
                5,
                java.time.LocalDateTime.now(clock)
        );

        when(allocationRepository.findTopByWorkDateLessThanEqualOrderByWorkDateDescCreatedAtDesc(eq(date)))
                .thenReturn(Optional.of(allocation));
        when(productivityRepository.findByWorkDate(date)).thenReturn(Optional.empty());
        when(productivityRepository.save(any(WorkdayProductivityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(routePlanRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(10L);
        when(routePlanRepository.countByDelayedTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(3L);
        when(outboxRepository.countByStatusIn(any(Collection.class))).thenReturn(4L);

        WorkdayProductivityResult result = service.runWorkday(date);

        assertThat(result.workforceEnabled()).isTrue();
        assertThat(result.baselineCapacity()).isFalse();
        assertThat(result.capacityEvents()).isEqualTo(2L);
        assertThat(result.workloadEvents()).isEqualTo(17L);
        assertThat(result.backlogEvents()).isEqualTo(15L);
        assertThat(result.status()).isEqualTo("BOTTLENECK_DETECTED");
        assertThat(result.bottleneckType()).isEqualTo("OUTBOX_PUBLISH_CAPACITY");
    }

    private WorkforceService service() {
        return new WorkforceService(
                allocationRepository,
                productivityRepository,
                routePlanRepository,
                outboxRepository,
                properties,
                idGenerator,
                clock
        );
    }
}
