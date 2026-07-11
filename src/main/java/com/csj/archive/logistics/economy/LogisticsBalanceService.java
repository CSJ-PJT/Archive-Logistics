package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.economy.model.LogisticsCostType;
import com.csj.archive.logistics.economy.model.LogisticsProfitSnapshotEntity;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.csj.archive.logistics.workforce.WorkforceService;
import com.csj.archive.logistics.workforce.WorkforceSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LogisticsBalanceService {
    private final LogisticsEconomyService economyService;
    private final LogisticsCostEventRepository costEventRepository;
    private final LogisticsProfitSnapshotRepository snapshotRepository;
    private final RoutePlanRepository routePlanRepository;
    private final WorkforceService workforceService;
    private final Clock clock;

    public LogisticsBalanceService(LogisticsEconomyService economyService,
                                   LogisticsCostEventRepository costEventRepository,
                                   LogisticsProfitSnapshotRepository snapshotRepository,
                                   RoutePlanRepository routePlanRepository,
                                   WorkforceService workforceService,
                                   Clock clock) {
        this.economyService = economyService;
        this.costEventRepository = costEventRepository;
        this.snapshotRepository = snapshotRepository;
        this.routePlanRepository = routePlanRepository;
        this.workforceService = workforceService;
        this.clock = clock;
    }

    public LogisticsBalanceSummaryResponse summary() {
        LogisticsEconomySummaryResponse economy = economyService.summary();
        WorkforceSummaryResponse workforce = workforceService.workforceSummary();
        long revenue = economy.totalRevenue();
        long fuel = cost(LogisticsCostType.LOGISTICS_FUEL_COST_INCURRED);
        long toll = cost(LogisticsCostType.LOGISTICS_TOLL_COST_INCURRED);
        long workforceCost = cost(LogisticsCostType.LOGISTICS_WORKFORCE_PAYROLL_COST_INCURRED);
        long delayCost = cost(LogisticsCostType.LOGISTICS_DELAY_PENALTY_COST_INCURRED,
                LogisticsCostType.DELIVERY_DELAY_OPERATION_COST_INCURRED);
        long coldChainCost = cost(LogisticsCostType.LOGISTICS_COLD_CHAIN_RISK_COST_INCURRED,
                LogisticsCostType.COLD_CHAIN_HANDLER_COST_INCURRED);
        long ledgerFee = cost(LogisticsCostType.LEDGER_SETTLEMENT_AGENCY_FEE_PAID,
                LogisticsCostType.LEDGER_RECONCILIATION_FEE_PAID);
        long requested = routePlanRepository.count();
        long completed = routePlanRepository.countByRouteStatus("DELIVERY_COMPLETED");
        long delayed = routePlanRepository.countByRouteStatus("DELIVERY_DELAYED");
        long dispatched = routePlanRepository.countByRouteStatusIn(List.of("DELIVERY_IN_TRANSIT", "DELIVERY_COMPLETED", "DELIVERY_DELAYED"));
        LocalDateTime calculatedAt = LocalDateTime.now(clock);
        if (requested == 0 && revenue == 0 && economy.totalCost() == 0 && !workforce.available()) {
            return new LogisticsBalanceSummaryResponse(
                    false, "NO_DATA", "No persisted synthetic logistics runtime or economy data is available.",
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null,
                    "PERSISTED_SYNTHETIC_RUNTIME_DATA", calculatedAt
            );
        }
        return new LogisticsBalanceSummaryResponse(
                true, "AVAILABLE", null,
                revenue, fuel, toll, workforceCost, delayCost, coldChainCost, ledgerFee, economy.totalCost(),
                revenue - economy.totalCost(), ratio(revenue - economy.totalCost(), revenue), economy.cashBalance(),
                requested, dispatched, delayed, completed, workforce.backlogCount(),
                ratio(workforce.usedCapacity(), workforce.effectiveCapacity()), workforce.bottleneckRole(),
                BigDecimal.valueOf(routePlanRepository.averageEstimatedMinutes() == null ? 0D : routePlanRepository.averageEstimatedMinutes()).setScale(2, RoundingMode.HALF_UP),
                ratio(delayed, requested), negativeProfitStreak(), "PERSISTED_SYNTHETIC_RUNTIME_DATA", calculatedAt
        );
    }

    private long cost(LogisticsCostType... types) {
        Long value = costEventRepository.sumCostByCostTypeIn(List.of(types));
        return value == null ? 0L : value;
    }

    private BigDecimal ratio(long value, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private int negativeProfitStreak() {
        int streak = 0;
        for (LogisticsProfitSnapshotEntity snapshot : snapshotRepository.findTop30ByOrderByCreatedAtDesc()) {
            if (snapshot.profitAmount() >= 0) {
                break;
            }
            streak++;
        }
        return streak;
    }
}
