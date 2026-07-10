package com.csj.archive.logistics.workforce;

import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.outbox.OutboxStatus;
import com.csj.archive.logistics.route.RoutePlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WorkforceService {
    private static final String SERVICE = "Archive-Logistics";

    private final WorkforceAllocationRepository allocationRepository;
    private final WorkdayProductivityRepository productivityRepository;
    private final RoutePlanRepository routePlanRepository;
    private final LogisticsOutboxRepository outboxRepository;
    private final WorkforceProperties properties;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public WorkforceService(WorkforceAllocationRepository allocationRepository,
                            WorkdayProductivityRepository productivityRepository,
                            RoutePlanRepository routePlanRepository,
                            LogisticsOutboxRepository outboxRepository,
                            WorkforceProperties properties,
                            IdGenerator idGenerator,
                            Clock clock) {
        this.allocationRepository = allocationRepository;
        this.productivityRepository = productivityRepository;
        this.routePlanRepository = routePlanRepository;
        this.outboxRepository = outboxRepository;
        this.properties = properties;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public WorkforceAllocationResponse assign(WorkforceAllocationRequest request) {
        LocalDate workDate = request.workDate() == null ? LocalDate.now(clock) : request.workDate();
        WorkforceAllocationRequest normalized = new WorkforceAllocationRequest(
                request.sourceService(),
                workDate,
                request.workdayId(),
                request.dispatchers(),
                request.drivers(),
                request.delayResponders(),
                request.simulationRunId(),
                request.settlementCycleId(),
                request.correlationId(),
                request.causationId(),
                request.hopCount(),
                request.maxHop(),
                request.reason()
        );
        String allocationId = "WF-ALLOC-" + workDate.toString().replace("-", "") + "-"
                + idGenerator.shortHash(normalized.normalizedSourceService() + ":" + workDate + ":" + LocalDateTime.now(clock));
        WorkforceAllocationEntity saved = allocationRepository.save(new WorkforceAllocationEntity(
                allocationId,
                normalized,
                laborCost(normalized.dispatchers(), normalized.drivers(), normalized.delayResponders()),
                properties.getMaxHop(),
                LocalDateTime.now(clock)
        ));
        return WorkforceAllocationResponse.from(saved);
    }

    @Transactional
    public WorkdayProductivityResult runWorkday(LocalDate date) {
        LocalDate workDate = date == null ? LocalDate.now(clock) : date;
        WorkdayProductivityResult result = calculate(workDate);
        WorkdayProductivityEntity saved = productivityRepository.findByWorkDate(workDate)
                .map(existing -> {
                    existing.apply(result, LocalDateTime.now(clock));
                    return existing;
                })
                .orElseGet(() -> productivityRepository.save(new WorkdayProductivityEntity(result, LocalDateTime.now(clock))));
        return WorkdayProductivityResult.from(saved);
    }

    @Transactional(readOnly = true)
    public WorkforceSummaryResponse workforceSummary() {
        WorkdayProductivityResult result = latestOrCalculated();
        return new WorkforceSummaryResponse(
                SERVICE,
                result.workforceEnabled(),
                result.baselineCapacity(),
                result.workDate(),
                result.allocationId(),
                result.dispatchers(),
                result.drivers(),
                result.delayResponders(),
                result.capacityEvents(),
                result.workloadEvents(),
                result.processedEvents(),
                result.backlogEvents(),
                result.shortageEvents(),
                result.syntheticLaborCost(),
                result.status(),
                result.bottleneckType()
        );
    }

    @Transactional(readOnly = true)
    public ProductivitySummaryResponse productivitySummary() {
        WorkdayProductivityResult result = latestOrCalculated();
        return new ProductivitySummaryResponse(
                SERVICE,
                result.workDate(),
                result.processedEvents(),
                result.capacityEvents(),
                result.productivityRate(),
                result.utilizationRate(),
                result.delayedResponseLoad(),
                result.status()
        );
    }

    @Transactional(readOnly = true)
    public CapacitySummaryResponse capacitySummary() {
        WorkdayProductivityResult result = latestOrCalculated();
        return new CapacitySummaryResponse(
                SERVICE,
                result.workDate(),
                result.workforceEnabled(),
                result.baselineCapacity(),
                result.capacityEvents(),
                result.workloadEvents(),
                result.backlogEvents(),
                result.shortageEvents(),
                result.bottleneckType()
        );
    }

    public WorkdayProductivityResult latestOrCalculated() {
        return productivityRepository.findTopByOrderByWorkDateDescCreatedAtDesc()
                .map(WorkdayProductivityResult::from)
                .orElseGet(() -> calculate(LocalDate.now(clock)));
    }

    private WorkdayProductivityResult calculate(LocalDate workDate) {
        Optional<WorkforceAllocationEntity> allocation = properties.isEnabled()
                ? allocationRepository.findTopByWorkDateLessThanEqualOrderByWorkDateDescCreatedAtDesc(workDate)
                : Optional.empty();

        boolean baseline = allocation.isEmpty();
        int dispatchers = allocation.map(WorkforceAllocationEntity::dispatchers).orElse(properties.getBaselineDispatchers());
        int drivers = allocation.map(WorkforceAllocationEntity::drivers).orElse(properties.getBaselineDrivers());
        int delayResponders = allocation.map(WorkforceAllocationEntity::delayResponders).orElse(properties.getBaselineDelayResponders());
        long capacity = capacity(dispatchers, drivers, delayResponders);
        long laborCost = allocation.map(WorkforceAllocationEntity::syntheticDailyLaborCost)
                .orElseGet(() -> laborCost(dispatchers, drivers, delayResponders));

        LocalDateTime start = workDate.atStartOfDay();
        LocalDateTime end = workDate.plusDays(1).atStartOfDay();
        long routeWorkload = routePlanRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        long delayedLoad = routePlanRepository.countByDelayedTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        long publishBacklog = outboxRepository.countByStatusIn(List.of(OutboxStatus.PENDING, OutboxStatus.RETRY));
        long workload = routeWorkload + delayedLoad + publishBacklog;
        long processed = Math.min(workload, capacity);
        long backlog = Math.max(0L, workload - capacity);
        String bottleneck = bottleneck(backlog, delayedLoad, publishBacklog);
        String status = backlog > 0 ? "BOTTLENECK_DETECTED" : "PRODUCTIVITY_REPORTED";
        String workdayId = "WORKDAY-" + workDate.toString().replace("-", "") + "-"
                + idGenerator.shortHash(SERVICE + ":" + workDate);

        return new WorkdayProductivityResult(
                workdayId,
                allocation.map(WorkforceAllocationEntity::allocationId).orElse(null),
                workDate,
                properties.isEnabled(),
                baseline,
                dispatchers,
                drivers,
                delayResponders,
                workload,
                capacity,
                processed,
                backlog,
                backlog,
                delayedLoad,
                ratio(processed, capacity),
                ratio(Math.min(workload, capacity), capacity),
                laborCost,
                status,
                bottleneck
        );
    }

    private long capacity(int dispatchers, int drivers, int delayResponders) {
        return (long) dispatchers * properties.getDispatcherDailyCapacity()
                + (long) drivers * properties.getDriverDailyCapacity()
                + (long) delayResponders * properties.getDelayResponderDailyCapacity();
    }

    private long laborCost(int dispatchers, int drivers, int delayResponders) {
        return (long) dispatchers * properties.getDispatcherDailyCost()
                + (long) drivers * properties.getDriverDailyCost()
                + (long) delayResponders * properties.getDelayResponderDailyCost();
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private String bottleneck(long backlog, long delayedLoad, long publishBacklog) {
        if (backlog <= 0) {
            return "NONE";
        }
        if (publishBacklog >= delayedLoad) {
            return "OUTBOX_PUBLISH_CAPACITY";
        }
        return "DELAY_RESPONSE_CAPACITY";
    }
}
