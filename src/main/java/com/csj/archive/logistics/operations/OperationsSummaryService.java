package com.csj.archive.logistics.operations;

import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.event.NexusEventRepository;
import com.csj.archive.logistics.event.NexusEventStatus;
import com.csj.archive.logistics.ledger.LedgerPublishProperties;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.outbox.OutboxStatus;
import com.csj.archive.logistics.route.RouteCostRepository;
import com.csj.archive.logistics.route.RoutePlanRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@Transactional(readOnly = true)
public class OperationsSummaryService {
    private final NexusEventRepository nexusEventRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteCostRepository routeCostRepository;
    private final LogisticsOutboxRepository outboxRepository;
    private final AuditLogService auditLogService;
    private final LedgerPublishProperties ledgerProperties;
    private final Environment environment;

    public OperationsSummaryService(NexusEventRepository nexusEventRepository,
                                    RoutePlanRepository routePlanRepository,
                                    RouteCostRepository routeCostRepository,
                                    LogisticsOutboxRepository outboxRepository,
                                    AuditLogService auditLogService,
                                    LedgerPublishProperties ledgerProperties,
                                    Environment environment) {
        this.nexusEventRepository = nexusEventRepository;
        this.routePlanRepository = routePlanRepository;
        this.routeCostRepository = routeCostRepository;
        this.outboxRepository = outboxRepository;
        this.auditLogService = auditLogService;
        this.ledgerProperties = ledgerProperties;
        this.environment = environment;
    }

    public OperationsSummaryResponse summary() {
        long failedEvents = nexusEventRepository.countByStatus(NexusEventStatus.FAILED);
        long outboxFailed = outboxRepository.countByStatus(OutboxStatus.FAILED);
        Runtime runtime = Runtime.getRuntime();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();
        return new OperationsSummaryResponse(
                "Archive-Logitics",
                failedEvents > 0 || outboxFailed > 0 ? "DEGRADED" : "HEALTHY",
                profile(),
                nexusEventRepository.count(),
                nexusEventRepository.countByStatus(NexusEventStatus.PROCESSED),
                auditLogService.countDuplicates(),
                failedEvents,
                routePlanRepository.count(),
                new OperationsSummaryResponse.Outbox(
                        outboxRepository.countByStatus(OutboxStatus.PENDING),
                        outboxRepository.countByStatus(OutboxStatus.PUBLISHED),
                        outboxFailed,
                        outboxRepository.countByStatus(OutboxStatus.RETRY),
                        outboxRepository.countByStatus(OutboxStatus.SKIPPED)
                ),
                new OperationsSummaryResponse.Risk(
                        routeCostRepository.countByRequiresApprovalTrue(),
                        routePlanRepository.countByDelayedTrue(),
                        routePlanRepository.countByDeviatedTrue(),
                        routePlanRepository.countByRequiresColdChainTrueAndDelayedTrue()
                ),
                new OperationsSummaryResponse.Ledger(
                        ledgerProperties.isEnabled(),
                        ledgerProperties.isEnabled() ? "ENABLED" : "DISABLED",
                        ledgerProperties.getBaseUrl(),
                        ledgerProperties.getBulkEndpoint(),
                        ledgerProperties.getContractMode().name()
                ),
                new OperationsSummaryResponse.Memory(
                        runtime.maxMemory() / 1024 / 1024,
                        usedHeap / 1024 / 1024
                )
        );
    }

    private String profile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 ? "default" : String.join(",", Arrays.asList(activeProfiles));
    }
}
