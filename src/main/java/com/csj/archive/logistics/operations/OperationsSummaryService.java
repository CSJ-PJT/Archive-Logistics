package com.csj.archive.logistics.operations;

import com.csj.archive.logistics.audit.AuditLogService;
import com.csj.archive.logistics.event.NexusEventRepository;
import com.csj.archive.logistics.event.NexusEventStatus;
import com.csj.archive.logistics.ledger.LedgerPublishProperties;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.outbox.OutboxStatus;
import com.csj.archive.logistics.route.RouteCostRepository;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.csj.archive.logistics.economy.LogisticsEconomyService;
import com.csj.archive.logistics.economy.LogisticsEconomySummaryResponse;
import com.csj.archive.logistics.economy.LogisticsBalanceService;
import com.csj.archive.logistics.runtime.RuntimeStatusResponse;
import com.csj.archive.logistics.runtime.RuntimeWorkLoop;
import com.csj.archive.logistics.workforce.WorkforceService;
import com.csj.archive.logistics.workforce.WorkforceSummaryResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class OperationsSummaryService {
    private final NexusEventRepository nexusEventRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteCostRepository routeCostRepository;
    private final LogisticsOutboxRepository outboxRepository;
    private final LogisticsEconomyService economyService;
    private final AuditLogService auditLogService;
    private final LedgerPublishProperties ledgerProperties;
    private final WorkforceService workforceService;
    private final RuntimeWorkLoop runtimeWorkLoop;
    private final LogisticsBalanceService balanceService;
    private final Environment environment;

    public OperationsSummaryService(NexusEventRepository nexusEventRepository,
                                    RoutePlanRepository routePlanRepository,
                                    RouteCostRepository routeCostRepository,
                                    LogisticsOutboxRepository outboxRepository,
                                    LogisticsEconomyService economyService,
                                    AuditLogService auditLogService,
                                    LedgerPublishProperties ledgerProperties,
                                    WorkforceService workforceService,
                                    RuntimeWorkLoop runtimeWorkLoop,
                                    LogisticsBalanceService balanceService,
                                    Environment environment) {
        this.nexusEventRepository = nexusEventRepository;
        this.routePlanRepository = routePlanRepository;
        this.routeCostRepository = routeCostRepository;
        this.outboxRepository = outboxRepository;
        this.economyService = economyService;
        this.auditLogService = auditLogService;
        this.ledgerProperties = ledgerProperties;
        this.workforceService = workforceService;
        this.runtimeWorkLoop = runtimeWorkLoop;
        this.balanceService = balanceService;
        this.environment = environment;
    }

    public OperationsSummaryResponse summary() {
        long failedEvents = nexusEventRepository.countByStatus(NexusEventStatus.FAILED);
        long outboxFailed = outboxRepository.countByStatus(OutboxStatus.FAILED);
        LogisticsEconomySummaryResponse economy = economyService.summary();
        WorkforceSummaryResponse workforce = workforceService.workforceSummary();
        RuntimeStatusResponse runtimeStatus = runtimeWorkLoop.status();
        String status = failedEvents > 0 || outboxFailed > 0 || workforce.backlogEvents() > 0 ? "DEGRADED" : "HEALTHY";
        String degradedReason = degradedReason(failedEvents, outboxFailed, workforce);
        Runtime runtime = Runtime.getRuntime();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();
        return new OperationsSummaryResponse(
                "Archive-Logistics",
                "Archive-Logistics",
                "Synthetic Logistics Event Backend",
                status,
                latestEventAt(),
                degradedReason,
                true,
                profile(),
                nexusEventRepository.count(),
                nexusEventRepository.countByStatus(NexusEventStatus.PROCESSED),
                auditLogService.countDuplicates(),
                failedEvents,
                routePlanRepository.count(),
                workforce.shipmentsRequested(),
                workforce.shipmentsDispatched(),
                workforce.shipmentsDelayed(),
                workforce.deliveryCompleted(),
                workforce.routePlansCreated(),
                workforce.backlogEvents(),
                new OperationsSummaryResponse.Economy(
                        economy.totalRevenue(),
                        economy.totalCost(),
                        economy.totalProfit(),
                        economy.cashBalance(),
                        economy.bankruptcyRisk()
                ),
                balanceService.summary(),
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
                new OperationsSummaryResponse.MarketOrigin(
                        routePlanRepository.countByOrderIdNotNull(),
                        routePlanRepository.countByExpressOrderTrue(),
                        routePlanRepository.countByCustomerType("VIP_CUSTOMER"),
                        routePlanRepository.countByHighRiskCustomerOrRiskLevel("HIGH_RISK_CUSTOMER", 4)
                ),
                new OperationsSummaryResponse.Workforce(
                        workforce.workforceEnabled(),
                        workforce.baselineCapacity(),
                        workforce.capacityEvents(),
                        workforce.workloadEvents(),
                        workforce.backlogEvents(),
                        workforce.shortageEvents(),
                        driverCapacity(workforce),
                        workforce.usedCapacity(),
                        workforce.bottleneckType(),
                        workforce.status(),
                        workforce.bottleneckType()
                ),
                new OperationsSummaryResponse.Runtime(
                        runtimeStatus.runtimeActive(),
                        runtimeStatus.autoRunEnabled(),
                        runtimeStatus.schedulerStatus(),
                        runtimeStatus.lastWorkAt(),
                        runtimeStatus.lastEventAt(),
                        runtimeStatus.eventsProducedLastTick(),
                        runtimeStatus.eventsConsumedLastTick(),
                        runtimeStatus.backlogCount(),
                        runtimeStatus.pipelineStatus()
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

    private LocalDateTime latestEventAt() {
        return Arrays.asList(
                        nexusEventRepository.latestCreatedAt(),
                        routePlanRepository.latestCreatedAt(),
                        outboxRepository.latestCreatedAt()
                ).stream()
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String degradedReason(long failedEvents, long outboxFailed, WorkforceSummaryResponse workforce) {
        if (failedEvents > 0) {
            return "Nexus logistics event failures detected.";
        }
        if (outboxFailed > 0) {
            return "Ledger outbox publish failures detected.";
        }
        if (workforce.backlogEvents() > 0) {
            return "Synthetic workforce backlog detected.";
        }
        return null;
    }

    private long driverCapacity(WorkforceSummaryResponse workforce) {
        return workforce.drivers() * 8L;
    }
}
