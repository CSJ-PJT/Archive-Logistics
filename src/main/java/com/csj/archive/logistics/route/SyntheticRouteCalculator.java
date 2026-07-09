package com.csj.archive.logistics.route;

import com.csj.archive.logistics.common.DeterministicHash;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.event.NexusLogisticsEventRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component
public class SyntheticRouteCalculator {
    private static final String CURRENCY = "KRW";
    private static final String REASON = "Synthetic logistics cost confirmed by Archive-Logitics";

    private final SyntheticDistanceMatrix distanceMatrix;
    private final DeterministicHash hash;
    private final IdGenerator idGenerator;

    public SyntheticRouteCalculator(SyntheticDistanceMatrix distanceMatrix, DeterministicHash hash, IdGenerator idGenerator) {
        this.distanceMatrix = distanceMatrix;
        this.hash = hash;
        this.idGenerator = idGenerator;
    }

    public RoutePlan calculate(NexusLogisticsEventRequest request) {
        NexusLogisticsEventRequest.Payload payload = request.payload();
        BigDecimal distanceKm = distanceMatrix.distanceKm(payload.originCode(), payload.destinationCode())
                .setScale(2, RoundingMode.UNNECESSARY);
        String priority = normalizePriority(payload.priority());
        int estimatedMinutes = estimatedMinutes(distanceKm, priority);
        BigDecimal riskScore = BigDecimal.valueOf(hash.zeroToOne(request.eventId() + ":" + request.idempotencyKey()))
                .setScale(4, RoundingMode.HALF_UP);
        boolean delayed = riskScore.compareTo(BigDecimal.valueOf(0.75)) >= 0;
        boolean deviated = riskScore.compareTo(BigDecimal.valueOf(0.90)) >= 0;

        long fuelCost = Math.round(distanceKm.doubleValue() * 1450);
        long tollCost = Math.round(distanceKm.doubleValue() * (distanceKm.compareTo(BigDecimal.valueOf(100)) > 0 ? 120 : 60));
        long urgentSurcharge = switch (priority) {
            case "HIGH" -> 30_000L;
            case "CRITICAL" -> 70_000L;
            default -> 0L;
        };
        long delayPenalty = delayed ? 50_000L : 0L;
        long coldChainPenalty = payload.requiresColdChain() && delayed ? 80_000L : 0L;
        long totalCost = fuelCost + tollCost + urgentSurcharge + delayPenalty + coldChainPenalty;
        boolean requiresApproval = totalCost >= 300_000L
                || riskScore.compareTo(BigDecimal.valueOf(0.85)) >= 0
                || "CRITICAL".equals(priority)
                || payload.requiresColdChain() && delayed;

        String routePlanId = idGenerator.routePlanId(request.eventId(), request.idempotencyKey(), request.occurredAt());
        String vendorId = vendorId(request.eventId(), request.idempotencyKey());

        return new RoutePlan(
                routePlanId,
                request.eventId(),
                payload.shipmentId(),
                payload.factoryId(),
                payload.originCode(),
                payload.destinationCode(),
                vendorId,
                distanceKm,
                estimatedMinutes,
                priority,
                riskScore,
                delayed,
                deviated,
                payload.requiresColdChain(),
                "COST_CONFIRMED",
                new RouteCost(fuelCost, tollCost, urgentSurcharge, delayPenalty, coldChainPenalty, totalCost,
                        CURRENCY, requiresApproval, REASON)
        );
    }

    private int estimatedMinutes(BigDecimal distanceKm, String priority) {
        double minutes = distanceKm.doubleValue() * 1.6;
        if ("HIGH".equals(priority)) {
            minutes *= 0.9;
        } else if ("CRITICAL".equals(priority)) {
            minutes *= 0.85;
        }
        return Math.toIntExact(Math.round(minutes));
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "NORMAL";
        }
        String normalized = priority.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "HIGH", "CRITICAL" -> normalized;
            default -> "NORMAL";
        };
    }

    private String vendorId(String eventId, String idempotencyKey) {
        return hash.bounded(eventId + ":" + idempotencyKey + ":vendor", 2) == 0
                ? "VENDOR-LOGISTICS-01"
                : "VENDOR-LOGISTICS-02";
    }
}
