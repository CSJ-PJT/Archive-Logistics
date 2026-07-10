package com.csj.archive.logistics.workforce;

import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.economy.LogisticsEconomyService;
import com.csj.archive.logistics.economy.model.LogisticsCostType;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private final LogisticsEconomyService economyService;

    public WorkforceService(WorkforceAllocationRepository allocationRepository,
                            WorkdayProductivityRepository productivityRepository,
                            RoutePlanRepository routePlanRepository,
                            LogisticsOutboxRepository outboxRepository,
                            WorkforceProperties properties,
                            IdGenerator idGenerator,
                            Clock clock,
                            LogisticsEconomyService economyService) {
        this.allocationRepository = allocationRepository;
        this.productivityRepository = productivityRepository;
        this.routePlanRepository = routePlanRepository;
        this.outboxRepository = outboxRepository;
        this.properties = properties;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.economyService = economyService;
    }

    @Transactional
    public WorkforceAllocationResponse assign(WorkforceAllocationRequest request) {
        LocalDate workDate = request.workDate() == null ? LocalDate.now(clock) : request.workDate();
        String workdayId = request.workdayId() == null || request.workdayId().isBlank()
                ? workdayId(workDate)
                : request.workdayId();
        WorkforceAllocationRequest normalized = new WorkforceAllocationRequest(
                request.sourceService(),
                request.targetService(),
                workDate,
                workdayId,
                request.roles(),
                request.dispatchers(),
                request.routePlanners(),
                request.drivers(),
                request.delayResponders(),
                request.coldChainHandlers(),
                request.logisticsManagers(),
                request.simulationRunId(),
                request.settlementCycleId(),
                request.correlationId(),
                request.causationId(),
                request.hopCount(),
                request.maxHop(),
                request.reason()
        );
        List<WorkforceAllocationEntity> saved = new ArrayList<>();
        for (WorkforceAllocationRequest.RoleAllocation role : roleAllocations(normalized)) {
            if (allocationRepository.existsByWorkdayIdAndRoleType(workdayId, role.roleType().name())) {
                continue;
            }
            String allocationId = "WF-ALLOC-" + workDate.toString().replace("-", "") + "-"
                    + idGenerator.shortHash(workdayId + ":" + role.roleType().name());
            saved.add(allocationRepository.save(new WorkforceAllocationEntity(
                    allocationId,
                    normalized,
                    role.roleType(),
                    role.allocatedHeadcount(),
                    capacityPerPerson(role),
                    productivity(role),
                    wage(role),
                    properties.getMaxHop(),
                    LocalDateTime.now(clock)
            )));
        }
        if (saved.isEmpty()) {
            saved = allocationRepository.findByWorkDate(workDate).stream()
                    .filter(entity -> workdayId.equals(entity.workdayId()))
                    .sorted(Comparator.comparing(entity -> entity.roleType().name()))
                    .toList();
        }
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
        recordWorkforceCosts(result);
        return WorkdayProductivityResult.from(saved);
    }

    @Transactional(readOnly = true)
    public WorkforceSummaryResponse workforceSummary() {
        WorkdayProductivityResult result = latestOrCalculated();
        String degradedReason = result.backlogEvents() > 0 || result.shortageEvents() > 0
                ? "Synthetic workforce capacity shortage or backlog detected."
                : null;
        return new WorkforceSummaryResponse(
                SERVICE,
                SERVICE,
                true,
                result.workforceEnabled(),
                result.baselineCapacity(),
                result.workDate(),
                result.workdayId(),
                result.allocationId(),
                result.dispatchers(),
                result.drivers(),
                result.delayResponders(),
                result.capacityEvents(),
                result.usedCapacity(),
                result.remainingCapacity(),
                result.workloadEvents(),
                result.shipmentsRequested(),
                result.shipmentsDispatched(),
                result.shipmentsDelayed(),
                result.routePlansCreated(),
                result.deliveryCompleted(),
                result.processedEvents(),
                result.backlogEvents(),
                result.shortageEvents(),
                result.syntheticLaborCost(),
                result.dispatchers() + result.drivers() + result.delayResponders(),
                result.capacityEvents(),
                result.backlogEvents(),
                result.shipmentsDelayed(),
                result.bottleneckType(),
                result.productivityRate(),
                result.syntheticLaborCost(),
                result.workDate().atStartOfDay(),
                degradedReason,
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
                result.usedCapacity(),
                result.remainingCapacity(),
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
        List<WorkforceAllocationEntity> allocations = properties.isEnabled()
                ? allocationRepository.findByWorkDate(workDate)
                : List.of();
        boolean baseline = allocations.isEmpty();
        Map<LogisticsWorkforceRole, RoleCapacity> roles = baseline ? baselineRoles() : roleCapacity(allocations);
        int dispatchers = headcount(roles, LogisticsWorkforceRole.DISPATCH_PLANNER);
        int drivers = headcount(roles, LogisticsWorkforceRole.DELIVERY_DRIVER);
        int delayResponders = headcount(roles, LogisticsWorkforceRole.DELAY_RESPONSE_OPERATOR);
        long capacity = roles.values().stream().mapToLong(RoleCapacity::effectiveCapacity).sum();
        long laborCost = roles.values().stream().mapToLong(RoleCapacity::payrollCost).sum();

        LocalDateTime start = workDate.atStartOfDay();
        LocalDateTime end = workDate.plusDays(1).atStartOfDay();
        long routeWorkload = routePlanRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        long delayedLoad = routePlanRepository.countByDelayedTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        long publishBacklog = outboxRepository.countByStatusIn(List.of(OutboxStatus.PENDING, OutboxStatus.RETRY));
        long routeCapacity = effective(roles, LogisticsWorkforceRole.ROUTE_PLANNER);
        long driverCapacity = effective(roles, LogisticsWorkforceRole.DELIVERY_DRIVER);
        long delayCapacity = effective(roles, LogisticsWorkforceRole.DELAY_RESPONSE_OPERATOR);
        long coldChainCapacity = effective(roles, LogisticsWorkforceRole.COLD_CHAIN_HANDLER);
        long dispatchCapacity = effective(roles, LogisticsWorkforceRole.DISPATCH_PLANNER);
        long managerCapacity = effective(roles, LogisticsWorkforceRole.LOGISTICS_MANAGER);
        long workload = routeWorkload + delayedLoad + publishBacklog;
        long routePlansCreated = Math.min(routeWorkload, routeCapacity);
        long shipmentsDispatched = Math.min(routePlansCreated, dispatchCapacity + managerCapacity);
        long deliveryCompleted = Math.min(shipmentsDispatched, driverCapacity);
        long delayedShipments = Math.max(0L, routeWorkload - deliveryCompleted)
                + Math.max(0L, delayedLoad - delayCapacity)
                + Math.max(0L, delayedLoad - coldChainCapacity);
        long processed = Math.min(workload, capacity);
        long used = Math.min(capacity, routePlansCreated + shipmentsDispatched + deliveryCompleted + Math.min(delayedLoad, delayCapacity));
        long remaining = Math.max(0L, capacity - used);
        long backlog = Math.max(0L, workload - capacity);
        String bottleneck = bottleneck(routeWorkload, routeCapacity, driverCapacity, delayCapacity, coldChainCapacity, publishBacklog);
        String status = backlog > 0 || delayedShipments > 0 ? "BOTTLENECK_DETECTED" : "PRODUCTIVITY_REPORTED";
        String workdayId = workdayId(workDate);
        String allocationId = allocations.isEmpty() ? null : allocations.getFirst().allocationId();
        WorkdayProductivityResult result = new WorkdayProductivityResult(
                workdayId,
                allocationId,
                workDate,
                properties.isEnabled(),
                baseline,
                dispatchers,
                drivers,
                delayResponders,
                workload,
                capacity,
                used,
                remaining,
                routeWorkload,
                shipmentsDispatched,
                delayedShipments,
                routePlansCreated,
                deliveryCompleted,
                processed,
                backlog,
                backlog,
                delayedLoad,
                ratio(processed, capacity),
                ratio(used, capacity),
                laborCost,
                status,
                bottleneck
        );
        return result;
    }

    private List<WorkforceAllocationRequest.RoleAllocation> roleAllocations(WorkforceAllocationRequest request) {
        if (request.roles() != null && !request.roles().isEmpty()) {
            return request.roles();
        }
        return List.of(
                legacyRole(LogisticsWorkforceRole.DISPATCH_PLANNER, request.dispatchers(), properties.getDispatcherDailyCapacity(), properties.getDispatcherDailyCost()),
                legacyRole(LogisticsWorkforceRole.ROUTE_PLANNER, request.routePlanners(), 30, 190_000L),
                legacyRole(LogisticsWorkforceRole.DELIVERY_DRIVER, request.drivers(), properties.getDriverDailyCapacity(), properties.getDriverDailyCost()),
                legacyRole(LogisticsWorkforceRole.DELAY_RESPONSE_OPERATOR, request.delayResponders(), properties.getDelayResponderDailyCapacity(), properties.getDelayResponderDailyCost()),
                legacyRole(LogisticsWorkforceRole.COLD_CHAIN_HANDLER, request.coldChainHandlers(), 12, 210_000L),
                legacyRole(LogisticsWorkforceRole.LOGISTICS_MANAGER, request.logisticsManagers(), 50, 260_000L)
        ).stream().filter(role -> role.allocatedHeadcount() > 0).toList();
    }

    private WorkforceAllocationRequest.RoleAllocation legacyRole(LogisticsWorkforceRole role, Integer headcount, int capacity, long wage) {
        return new WorkforceAllocationRequest.RoleAllocation(role, headcount == null ? 0 : headcount, capacity, BigDecimal.ONE, wage);
    }

    private Map<LogisticsWorkforceRole, RoleCapacity> baselineRoles() {
        Map<LogisticsWorkforceRole, RoleCapacity> roles = new EnumMap<>(LogisticsWorkforceRole.class);
        roles.put(LogisticsWorkforceRole.DISPATCH_PLANNER, new RoleCapacity(properties.getBaselineDispatchers(), properties.getDispatcherDailyCapacity(), BigDecimal.ONE, properties.getDispatcherDailyCost()));
        roles.put(LogisticsWorkforceRole.ROUTE_PLANNER, new RoleCapacity(1, LogisticsWorkforceRole.ROUTE_PLANNER.defaultCapacityPerPersonPerDay(), BigDecimal.ONE, LogisticsWorkforceRole.ROUTE_PLANNER.defaultWagePerDay()));
        roles.put(LogisticsWorkforceRole.DELIVERY_DRIVER, new RoleCapacity(properties.getBaselineDrivers(), properties.getDriverDailyCapacity(), BigDecimal.ONE, properties.getDriverDailyCost()));
        roles.put(LogisticsWorkforceRole.DELAY_RESPONSE_OPERATOR, new RoleCapacity(properties.getBaselineDelayResponders(), properties.getDelayResponderDailyCapacity(), BigDecimal.ONE, properties.getDelayResponderDailyCost()));
        roles.put(LogisticsWorkforceRole.COLD_CHAIN_HANDLER, new RoleCapacity(1, LogisticsWorkforceRole.COLD_CHAIN_HANDLER.defaultCapacityPerPersonPerDay(), BigDecimal.ONE, LogisticsWorkforceRole.COLD_CHAIN_HANDLER.defaultWagePerDay()));
        roles.put(LogisticsWorkforceRole.LOGISTICS_MANAGER, new RoleCapacity(1, LogisticsWorkforceRole.LOGISTICS_MANAGER.defaultCapacityPerPersonPerDay(), BigDecimal.ONE, LogisticsWorkforceRole.LOGISTICS_MANAGER.defaultWagePerDay()));
        return roles;
    }

    private Map<LogisticsWorkforceRole, RoleCapacity> roleCapacity(List<WorkforceAllocationEntity> allocations) {
        Map<LogisticsWorkforceRole, RoleCapacity> roles = new EnumMap<>(LogisticsWorkforceRole.class);
        for (WorkforceAllocationEntity allocation : allocations) {
            roles.put(allocation.roleType(), new RoleCapacity(
                    allocation.allocatedHeadcount(),
                    allocation.capacityPerPersonPerDay(),
                    allocation.productivityScore(),
                    allocation.wagePerDay()
            ));
        }
        return roles;
    }

    private int headcount(Map<LogisticsWorkforceRole, RoleCapacity> roles, LogisticsWorkforceRole role) {
        return roles.getOrDefault(role, RoleCapacity.ZERO).headcount();
    }

    private long effective(Map<LogisticsWorkforceRole, RoleCapacity> roles, LogisticsWorkforceRole role) {
        return roles.getOrDefault(role, RoleCapacity.ZERO).effectiveCapacity();
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private String bottleneck(long routeWorkload, long routeCapacity, long driverCapacity, long delayCapacity, long coldChainCapacity, long publishBacklog) {
        if (routeWorkload <= routeCapacity && routeWorkload <= driverCapacity && routeWorkload <= delayCapacity && routeWorkload <= coldChainCapacity && publishBacklog == 0) {
            return "NONE";
        }
        if (routeWorkload > driverCapacity) {
            return LogisticsWorkforceRole.DELIVERY_DRIVER.name();
        }
        if (routeWorkload > routeCapacity) {
            return LogisticsWorkforceRole.ROUTE_PLANNER.name();
        }
        if (routeWorkload > delayCapacity) {
            return LogisticsWorkforceRole.DELAY_RESPONSE_OPERATOR.name();
        }
        if (routeWorkload > coldChainCapacity) {
            return LogisticsWorkforceRole.COLD_CHAIN_HANDLER.name();
        }
        if (publishBacklog > 0) {
            return "OUTBOX_PUBLISH_CAPACITY";
        }
        return "NONE";
    }

    private int capacityPerPerson(WorkforceAllocationRequest.RoleAllocation role) {
        return role.capacityPerPersonPerDay() == null ? role.roleType().defaultCapacityPerPersonPerDay() : role.capacityPerPersonPerDay();
    }

    private BigDecimal productivity(WorkforceAllocationRequest.RoleAllocation role) {
        return role.productivityScore() == null ? BigDecimal.ONE : role.productivityScore();
    }

    private long wage(WorkforceAllocationRequest.RoleAllocation role) {
        return role.wagePerDay() == null ? role.roleType().defaultWagePerDay() : role.wagePerDay();
    }

    private String workdayId(LocalDate workDate) {
        return "WORKDAY-" + workDate.toString().replace("-", "") + "-" + idGenerator.shortHash(SERVICE + ":" + workDate);
    }

    private void recordWorkforceCosts(WorkdayProductivityResult result) {
        LocalDateTime now = LocalDateTime.now(clock);
        economyService.recordWorkforceCost(
                result.workdayId(),
                result.workdayId(),
                result.allocationId() == null ? result.workdayId() : result.allocationId(),
                null,
                null,
                LogisticsCostType.LOGISTICS_WORKFORCE_PAYROLL_COST_INCURRED,
                result.syntheticLaborCost(),
                "Synthetic Logistics workforce payroll cost.",
                now
        );
        economyService.recordWorkforceCost(
                result.workdayId() + ":BACKLOG",
                result.workdayId(),
                result.workdayId(),
                null,
                null,
                LogisticsCostType.LOGISTICS_BACKLOG_COST_INCURRED,
                result.backlogEvents() * 2_000L,
                "Synthetic Logistics backlog carrying cost.",
                now
        );
        economyService.recordWorkforceCost(
                result.workdayId() + ":DELAY",
                result.workdayId(),
                result.workdayId(),
                null,
                null,
                LogisticsCostType.DELIVERY_DELAY_OPERATION_COST_INCURRED,
                result.shipmentsDelayed() * 3_000L,
                "Synthetic delivery delay operation cost.",
                now
        );
    }

    private record RoleCapacity(int headcount, int capacityPerPerson, BigDecimal productivityScore, long wagePerDay) {
        private static final RoleCapacity ZERO = new RoleCapacity(0, 0, BigDecimal.ZERO, 0L);

        long effectiveCapacity() {
            return BigDecimal.valueOf((long) headcount * capacityPerPerson)
                    .multiply(productivityScore)
                    .longValue();
        }

        long payrollCost() {
            return (long) headcount * wagePerDay;
        }
    }
}
