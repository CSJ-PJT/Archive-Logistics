package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.economy.model.LogisticsProfitSnapshotEntity;
import com.csj.archive.logistics.economy.properties.LogisticsEconomyProperties;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogisticsEconomyServiceTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-20T03:00:00Z");

    private final LogisticsRevenueEventRepository revenueEventRepository = mock(LogisticsRevenueEventRepository.class);
    private final LogisticsCostEventRepository costEventRepository = mock(LogisticsCostEventRepository.class);
    private final LogisticsProfitSnapshotRepository snapshotRepository = mock(LogisticsProfitSnapshotRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final LogisticsOutboxRepository outboxRepository = mock(LogisticsOutboxRepository.class);
    private final LogisticsEconomyProperties properties = new LogisticsEconomyProperties();
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Test
    void snapshotCashIsStableForReplayAndMovesOnlyByCumulativeProfitDelta() {
        properties.setOpeningCashBalance(5_000_000L);
        when(revenueEventRepository.sumRevenue()).thenReturn(1_000_000L, 1_000_000L, 1_250_000L);
        when(costEventRepository.sumCost()).thenReturn(400_000L, 400_000L, 450_000L);
        when(idGenerator.shortHash(any())).thenReturn("stable-id");
        when(snapshotRepository.save(any(LogisticsProfitSnapshotEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LogisticsEconomyService service = service();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate settlementDate = now.toLocalDate();

        service.recordSnapshot(settlementDate, "replayable snapshot", now);
        service.recordSnapshot(settlementDate, "replayable snapshot", now);
        service.recordSnapshot(settlementDate, "replayable snapshot", now.plusMinutes(1));

        ArgumentCaptor<LogisticsProfitSnapshotEntity> captor =
                ArgumentCaptor.forClass(LogisticsProfitSnapshotEntity.class);
        verify(snapshotRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        List<LogisticsProfitSnapshotEntity> snapshots = captor.getAllValues();

        assertThat(snapshots).extracting(LogisticsProfitSnapshotEntity::snapshotId)
                .containsOnly("SNAP-20260820-stable-id");
        assertThat(snapshots).extracting(LogisticsProfitSnapshotEntity::profitAmount)
                .containsExactly(600_000L, 600_000L, 800_000L);
        assertThat(snapshots).extracting(LogisticsProfitSnapshotEntity::cashBalance)
                .containsExactly(5_600_000L, 5_600_000L, 5_800_000L);
        assertThat(snapshots.get(2).cashBalance() - snapshots.get(1).cashBalance())
                .isEqualTo(snapshots.get(2).profitAmount() - snapshots.get(1).profitAmount());
        verify(snapshotRepository, never()).findTopByOrderByCreatedAtDesc();
    }

    @Test
    void summaryKeepsLegacyLifetimeTotalsAndAddsExplicitLast24HourPnlContext() {
        LocalDateTime asOf = LocalDateTime.now(clock);
        LocalDateTime fromInclusive = asOf.minusHours(24);
        LogisticsProfitSnapshotEntity latest = new LogisticsProfitSnapshotEntity(
                "SNAP-20260820-latest",
                asOf.toLocalDate(),
                9_000_000L,
                4_000_000L,
                5_000_000L,
                10_000_000L,
                "LOW",
                asOf.minusMinutes(5)
        );
        when(revenueEventRepository.sumRevenue()).thenReturn(9_000_000L);
        when(costEventRepository.sumCost()).thenReturn(4_000_000L);
        when(revenueEventRepository.sumRevenueBetween(eq(fromInclusive), eq(asOf))).thenReturn(900_000L);
        when(costEventRepository.sumCostBetween(eq(fromInclusive), eq(asOf))).thenReturn(350_000L);
        when(revenueEventRepository.findLatestCreatedAtBetween(eq(fromInclusive), eq(asOf)))
                .thenReturn(Optional.of(asOf.minusMinutes(10)));
        when(costEventRepository.findLatestCreatedAtBetween(eq(fromInclusive), eq(asOf)))
                .thenReturn(Optional.of(asOf.minusMinutes(5)));
        when(snapshotRepository.findTopByOrderByCreatedAtDesc()).thenReturn(Optional.of(latest));

        LogisticsEconomySummaryResponse summary = service().summary();

        assertThat(summary.totalRevenue()).isEqualTo(9_000_000L);
        assertThat(summary.totalCost()).isEqualTo(4_000_000L);
        assertThat(summary.totalProfit()).isEqualTo(5_000_000L);
        assertThat(summary.cashBalance()).isEqualTo(10_000_000L);
        assertThat(summary.currency()).isEqualTo("SYNTHETIC_KRW");
        assertThat(summary.scope()).isEqualTo("ROLLING_24H_RECOGNIZED_LOGISTICS_EVENTS");
        assertThat(summary.window()).isEqualTo("LAST_24_HOURS");
        assertThat(summary.asOf()).isEqualTo(asOf);
        assertThat(summary.recognizedRevenue()).isEqualTo(900_000L);
        assertThat(summary.realizedOperatingCost()).isEqualTo(350_000L);
        assertThat(summary.operatingProfit()).isEqualTo(550_000L);
        assertThat(summary.calculationScope()).isEqualTo("ROLLING_24H_RECOGNIZED_LOGISTICS_EVENTS");
        assertThat(summary.periodStart()).isEqualTo(FIXED_INSTANT.minusSeconds(24 * 60 * 60));
        assertThat(summary.periodEnd()).isEqualTo(FIXED_INSTANT);
        assertThat(summary.dataAvailable()).isTrue();
        assertThat(summary.sourceLatestEventAt()).isEqualTo(FIXED_INSTANT.minusSeconds(5 * 60));
        assertThat(summary.legacyCalculationScope()).isEqualTo("LIFETIME_PERSISTED_LOGISTICS_SNAPSHOT_FIELDS");
    }

    @Test
    void summaryMarksCurrentFinanceUnavailableWhenOnlyLifetimeSnapshotsExist() {
        LocalDateTime asOf = LocalDateTime.now(clock);
        LocalDateTime fromInclusive = asOf.minusHours(24);
        when(revenueEventRepository.sumRevenue()).thenReturn(9_000_000L);
        when(costEventRepository.sumCost()).thenReturn(4_000_000L);
        when(revenueEventRepository.sumRevenueBetween(eq(fromInclusive), eq(asOf))).thenReturn(0L);
        when(costEventRepository.sumCostBetween(eq(fromInclusive), eq(asOf))).thenReturn(0L);
        when(revenueEventRepository.findLatestCreatedAtBetween(eq(fromInclusive), eq(asOf))).thenReturn(Optional.empty());
        when(costEventRepository.findLatestCreatedAtBetween(eq(fromInclusive), eq(asOf))).thenReturn(Optional.empty());
        when(snapshotRepository.findTopByOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        LogisticsEconomySummaryResponse summary = service().summary();

        assertThat(summary.totalRevenue()).isEqualTo(9_000_000L);
        assertThat(summary.recognizedRevenue()).isZero();
        assertThat(summary.operatingProfit()).isZero();
        assertThat(summary.dataAvailable()).isFalse();
        assertThat(summary.sourceLatestEventAt()).isNull();
    }

    private LogisticsEconomyService service() {
        return new LogisticsEconomyService(
                revenueEventRepository,
                costEventRepository,
                snapshotRepository,
                auditLogService,
                outboxRepository,
                properties,
                idGenerator,
                clock
        );
    }
}
