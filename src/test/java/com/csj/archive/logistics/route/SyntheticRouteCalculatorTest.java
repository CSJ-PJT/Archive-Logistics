package com.csj.archive.logistics.route;

import com.csj.archive.logistics.common.DeterministicHash;
import com.csj.archive.logistics.common.IdGenerator;
import com.csj.archive.logistics.event.NexusLogisticsEventRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SyntheticRouteCalculatorTest {
    private final DeterministicHash hash = new DeterministicHash();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);
    private final SyntheticRouteCalculator calculator = new SyntheticRouteCalculator(
            new SyntheticDistanceMatrix(),
            hash,
            new IdGenerator(clock, hash)
    );

    @Test
    void highPriorityAddsUrgentSurchargeAndReducesEta() {
        RoutePlan route = calculator.calculate(event("001", "FAC-A", "DC-SEOUL-01", "HIGH", false));

        assertThat(route.distanceKm()).isEqualByComparingTo("42.00");
        assertThat(route.estimatedMinutes()).isEqualTo(60);
        assertThat(route.cost().fuelCost()).isEqualTo(60_900L);
        assertThat(route.cost().tollCost()).isEqualTo(2_520L);
        assertThat(route.cost().urgentSurcharge()).isEqualTo(30_000L);
    }

    @Test
    void criticalPriorityRequiresApproval() {
        RoutePlan route = calculator.calculate(event("002", "FAC-A", "DC-SEOUL-01", "CRITICAL", false));

        assertThat(route.cost().requiresApproval()).isTrue();
        assertThat(route.cost().urgentSurcharge()).isEqualTo(70_000L);
    }

    @Test
    void sameInputAlwaysProducesSameRiskAndTotalCost() {
        NexusLogisticsEventRequest request = event("003", "FAC-B", "DC-BUSAN-01", "HIGH", false);

        RoutePlan first = calculator.calculate(request);
        RoutePlan second = calculator.calculate(request);

        assertThat(first.riskScore()).isEqualByComparingTo(second.riskScore());
        assertThat(first.cost().totalCost()).isEqualTo(second.cost().totalCost());
        assertThat(first.routePlanId()).isEqualTo(second.routePlanId());
    }

    @Test
    void delayedColdChainAddsPenaltyAndRequiresApproval() {
        RoutePlan route = null;
        for (int index = 0; index < 10_000; index++) {
            RoutePlan candidate = calculator.calculate(event("cold-" + index, "FAC-C", "DC-BUSAN-01", "NORMAL", true));
            if (candidate.delayed()) {
                route = candidate;
                break;
            }
        }

        assertThat(route).isNotNull();
        assertThat(route.delayed()).isTrue();
        assertThat(route.cost().coldChainPenalty()).isEqualTo(80_000L);
        assertThat(route.cost().requiresApproval()).isTrue();
    }

    private NexusLogisticsEventRequest event(String suffix, String origin, String destination, String priority, boolean coldChain) {
        String shipmentId = "SHIP-" + suffix;
        return new NexusLogisticsEventRequest(
                "evt-nexus-test-" + suffix,
                "NEXUS:LOGISTICS_DISPATCHED:" + origin + ":" + shipmentId,
                "Archive-Nexus",
                "LOGISTICS_DISPATCHED",
                Instant.parse("2026-01-15T10:32:15Z"),
                new NexusLogisticsEventRequest.Payload(
                        origin,
                        shipmentId,
                        origin,
                        destination,
                        priority,
                        "battery-module",
                        120,
                        coldChain
                )
        );
    }
}
