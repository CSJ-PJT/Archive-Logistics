package com.csj.archive.logistics.route;

import com.csj.archive.logistics.common.NotFoundException;
import com.csj.archive.logistics.common.PageResponse;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RoutePlanService {
    private final RoutePlanRepository routePlanRepository;
    private final RouteCostRepository routeCostRepository;
    private final LogisticsOutboxRepository outboxRepository;

    public RoutePlanService(RoutePlanRepository routePlanRepository,
                            RouteCostRepository routeCostRepository,
                            LogisticsOutboxRepository outboxRepository) {
        this.routePlanRepository = routePlanRepository;
        this.routeCostRepository = routeCostRepository;
        this.outboxRepository = outboxRepository;
    }

    public PageResponse<RouteSummaryResponse> plans(String factoryId, Pageable pageable) {
        Page<RoutePlanEntity> page = factoryId == null || factoryId.isBlank()
                ? routePlanRepository.findAll(pageable)
                : routePlanRepository.findByFactoryId(factoryId, pageable);
        return PageResponse.from(page.map(this::toSummary));
    }

    public RouteSummaryResponse plan(String routePlanId) {
        return routePlanRepository.findByRoutePlanId(routePlanId)
                .map(this::toSummary)
                .orElseThrow(() -> new NotFoundException("Route plan not found: " + routePlanId));
    }

    public PageResponse<RouteCostResponse> costs(Pageable pageable) {
        return PageResponse.from(routeCostRepository.findAll(pageable).map(this::toCostResponse));
    }

    public RouteCostResponse cost(String routePlanId) {
        return routeCostRepository.findByRoutePlanId(routePlanId)
                .map(this::toCostResponse)
                .orElseThrow(() -> new NotFoundException("Route cost not found: " + routePlanId));
    }

    public RouteAggregateSummaryResponse summary(LocalDate date, String factoryId) {
        LocalDateTime start = date == null ? null : date.atStartOfDay();
        LocalDateTime end = date == null ? null : date.plusDays(1).atStartOfDay();
        String factory = blankToNull(factoryId);
        var plans = (date == null)
                ? (factory == null ? routePlanRepository.findAll()
                    : routePlanRepository.findByFactoryId(factory))
                : (factory == null ? routePlanRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)
                    : routePlanRepository.findByFactoryIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(factory, start, end));
        var costs = routeCostRepository.findByRoutePlanIdIn(plans.stream().map(RoutePlanEntity::routePlanId).toList())
                .stream()
                .collect(Collectors.toMap(RouteCostEntity::routePlanId, Function.identity()));

        long delayed = plans.stream().filter(RoutePlanEntity::delayed).count();
        long deviated = plans.stream().filter(RoutePlanEntity::deviated).count();
        long coldChainRisk = plans.stream().filter(plan -> plan.requiresColdChain() && plan.delayed()).count();
        long approvalRequired = costs.values().stream().filter(RouteCostEntity::requiresApproval).count();
        long totalCost = costs.values().stream().mapToLong(RouteCostEntity::totalCost).sum();
        return new RouteAggregateSummaryResponse(date, factory, plans.size(), delayed, deviated,
                approvalRequired, coldChainRisk, totalCost);
    }

    private RouteSummaryResponse toSummary(RoutePlanEntity plan) {
        RouteCostEntity cost = routeCostRepository.findByRoutePlanId(plan.routePlanId()).orElse(null);
        String outboxStatus = outboxRepository.findByAggregateId(plan.routePlanId())
                .map(outbox -> outbox.status().name())
                .orElse("NONE");
        return new RouteSummaryResponse(
                plan.routePlanId(),
                plan.sourceEventId(),
                plan.shipmentId(),
                plan.factoryId(),
                plan.originCode(),
                plan.destinationCode(),
                plan.distanceKm(),
                plan.estimatedMinutes(),
                plan.priority(),
                plan.riskScore(),
                plan.delayed(),
                plan.deviated(),
                cost == null ? null : cost.totalCost(),
                cost != null && cost.requiresApproval(),
                outboxStatus
        );
    }

    private RouteCostResponse toCostResponse(RouteCostEntity cost) {
        return new RouteCostResponse(
                cost.routePlanId(),
                cost.fuelCost(),
                cost.tollCost(),
                cost.urgentSurcharge(),
                cost.delayPenalty(),
                cost.coldChainPenalty(),
                cost.totalCost(),
                cost.currency(),
                cost.requiresApproval(),
                cost.reason()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
