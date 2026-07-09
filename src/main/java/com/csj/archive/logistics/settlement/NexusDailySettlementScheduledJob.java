package com.csj.archive.logistics.settlement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class NexusDailySettlementScheduledJob {
    private static final Logger log = LoggerFactory.getLogger(NexusDailySettlementScheduledJob.class);

    private final NexusDailySettlementService settlementService;
    private final Clock clock;
    private final boolean enabled;
    private final long dateOffsetDays;

    public NexusDailySettlementScheduledJob(NexusDailySettlementService settlementService,
                                            Clock clock,
                                            @Value("${archive.nexus-settlement.scheduler.enabled:false}") boolean enabled,
                                            @Value("${archive.nexus-settlement.scheduler.date-offset-days:0}") long dateOffsetDays) {
        this.settlementService = settlementService;
        this.clock = clock;
        this.enabled = enabled;
        this.dateOffsetDays = dateOffsetDays;
    }

    @Scheduled(
            initialDelayString = "${archive.nexus-settlement.scheduler.initial-delay-ms:45000}",
            fixedDelayString = "${archive.nexus-settlement.scheduler.fixed-delay-ms:300000}"
    )
    public void runIfReady() {
        if (!enabled) {
            return;
        }
        LocalDate date = LocalDate.now(clock).minusDays(dateOffsetDays);
        try {
            if (!settlementService.hasRunnableSettlementCandidates(date)) {
                return;
            }
            NexusDailySettlementRunResult result = settlementService.run(date, null);
            log.info("Scheduled Nexus daily settlement completed for {}: sent={}, dryRun={}, retry={}, failed={}, skipped={}",
                    date,
                    result.sentCount(),
                    result.dryRunCount(),
                    result.retryCount(),
                    result.failedCount(),
                    result.skippedCount());
        } catch (RuntimeException error) {
            log.warn("Scheduled Nexus daily settlement failed for {}: {}", date, error.getMessage());
        }
    }
}
