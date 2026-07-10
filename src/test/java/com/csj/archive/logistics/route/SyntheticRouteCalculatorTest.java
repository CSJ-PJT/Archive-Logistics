package com.csj.archive.logistics.route;

import com.csj.archive.logistics.event.MarketShipmentMetadata;
import com.csj.archive.logistics.event.NexusLogisticsEventRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class SyntheticRouteCalculatorTest {
    private final com.csj.archive.logistics.common.DeterministicHash hash = new com.csj.archive.logistics.common.DeterministicHash();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);
    private final SyntheticRouteCalculator calculator = new SyntheticRouteCalculator(
            new SyntheticDistanceMatrix(),
            hash,
            new com.csj.archive.logistics.common.IdGenerator(clock, hash)
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

    @Test
    void expressOrderMetadataAppliesUrgentSurchargeEvenWithoutPriority() {
        NexusLogisticsEventRequest request = event(
                "express-1",
                "FAC-A",
                "DC-SEOUL-01",
                "NORMAL",
                false,
                metadataBuilder()
                        .expressOrder(true)
                        .marketPriority("NORMAL")
        );

        RoutePlan route = calculator.calculate(request, MarketShipmentMetadata.fromPayload(request.payload()));

        assertThat(route.marketPriority()).isEqualTo("NORMAL");
        assertThat(route.cost().urgentSurcharge()).isEqualTo(30_000L);
    }

    @Test
    void highRiskMetadataIncreasesRiskScore() {
        RoutePlan base = null;
        RoutePlan riskAdjusted = null;
        for (int index = 0; index < 2_000; index++) {
            NexusLogisticsEventRequest request = event(
                    "risk-" + index,
                    "FAC-B",
                    "DC-DAEJEON-01",
                    "NORMAL",
                    false,
                    metadataBuilder()
                            .riskLevel(2)
                            .riskTag("NORMAL_RISK_CUSTOMER")
            );
            RoutePlan normal = calculator.calculate(request);
            if (normal.riskScore().compareTo(BigDecimal.valueOf(0.80)) < 0) {
                base = normal;
                riskAdjusted = calculator.calculate(event(
                        "risk-" + index,
                        "FAC-B",
                        "DC-DAEJEON-01",
                        "NORMAL",
                        false,
                        metadataBuilder()
                                .riskLevel(10)
                                .riskTag("HIGH_RISK_CUSTOMER")
                ));
                break;
            }
        }
        if (base == null || riskAdjusted == null) {
            fail("Unable to find deterministic sample for high-risk riskScore comparison");
        }
        assertThat(riskAdjusted.riskScore()).isGreaterThan(base.riskScore());
    }

    private NexusLogisticsEventRequest event(String suffix, String origin, String destination, String priority, boolean coldChain) {
        return event(suffix, origin, destination, priority, coldChain, null);
    }

    private NexusLogisticsEventRequest event(String suffix, String origin, String destination, String priority, boolean coldChain,
                                            MarketMetadataBuilder metadataBuilder) {
        String shipmentId = "SHIP-" + suffix;
        MarketMetadataBuilder builder = metadataBuilder == null ? metadataBuilder() : metadataBuilder;
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
                        coldChain,
                        builder.orderId,
                        builder.customerId,
                        builder.customerType,
                        builder.productType,
                        builder.orderAmount,
                        builder.totalAmount,
                        builder.riskLevel,
                        builder.expressOrder,
                        builder.riskTag,
                        builder.vipCustomer,
                        builder.simulationRunId,
                        builder.settlementCycleId,
                        builder.correlationId,
                        builder.causationId,
                        builder.hopCount,
                        builder.maxHop,
                        builder.marketPriority
                )
        );
    }

    private MarketMetadataBuilder metadataBuilder() {
        return new MarketMetadataBuilder();
    }

    private static class MarketMetadataBuilder {
        String orderId = null;
        String customerId = null;
        String customerType = "NORMAL_CUSTOMER";
        String productType = "battery-module";
        Long orderAmount = null;
        Long totalAmount = null;
        Integer riskLevel = null;
        Boolean expressOrder = false;
        String riskTag = null;
        Boolean vipCustomer = null;
        String simulationRunId = null;
        String settlementCycleId = null;
        String correlationId = "corr-1";
        String causationId = "cause-1";
        Integer hopCount = null;
        Integer maxHop = null;
        String marketPriority = "NORMAL";

        MarketMetadataBuilder orderId(String value) { orderId = value; return this; }
        MarketMetadataBuilder customerId(String value) { customerId = value; return this; }
        MarketMetadataBuilder customerType(String value) { customerType = value; return this; }
        MarketMetadataBuilder productType(String value) { productType = value; return this; }
        MarketMetadataBuilder orderAmount(Long value) { orderAmount = value; return this; }
        MarketMetadataBuilder totalAmount(Long value) { totalAmount = value; return this; }
        MarketMetadataBuilder riskLevel(Integer value) { riskLevel = value; return this; }
        MarketMetadataBuilder expressOrder(Boolean value) { expressOrder = value; return this; }
        MarketMetadataBuilder riskTag(String value) { riskTag = value; return this; }
        MarketMetadataBuilder vipCustomer(Boolean value) { vipCustomer = value; return this; }
        MarketMetadataBuilder simulationRunId(String value) { simulationRunId = value; return this; }
        MarketMetadataBuilder settlementCycleId(String value) { settlementCycleId = value; return this; }
        MarketMetadataBuilder correlationId(String value) { correlationId = value; return this; }
        MarketMetadataBuilder causationId(String value) { causationId = value; return this; }
        MarketMetadataBuilder hopCount(Integer value) { hopCount = value; return this; }
        MarketMetadataBuilder maxHop(Integer value) { maxHop = value; return this; }
        MarketMetadataBuilder marketPriority(String value) { marketPriority = value; return this; }
    }
}
