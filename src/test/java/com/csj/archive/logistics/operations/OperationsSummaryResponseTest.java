package com.csj.archive.logistics.operations;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OperationsSummaryResponseTest {
    @Test
    void operationsSummaryContainsLiveFlowFields() throws Exception {
        OperationsSummaryResponse response = new OperationsSummaryResponse(
                "Archive-Logistics",
                "Archive-Logistics",
                "Synthetic Logistics Event Backend",
                "HEALTHY",
                LocalDateTime.parse("2026-07-10T10:00:00"),
                null,
                true,
                "local",
                1,
                1,
                0,
                0,
                1,
                10,
                8,
                2,
                8,
                10,
                1,
                new OperationsSummaryResponse.Economy(100, 50, 50, 1_000, "LOW"),
                new com.csj.archive.logistics.economy.LogisticsBalanceSummaryResponse(
                        true, "AVAILABLE", null, 100L, 10L, 5L, 20L, 0L, 0L, 0L, 50L, 50L,
                        java.math.BigDecimal.valueOf(0.5), 1_000L, 10L, 8L, 2L, 8L, 1L,
                        java.math.BigDecimal.valueOf(0.6), "NONE", java.math.BigDecimal.valueOf(60),
                        java.math.BigDecimal.valueOf(0.2), 0, "PERSISTED_SYNTHETIC_RUNTIME_DATA", LocalDateTime.parse("2026-07-10T10:00:00")
                ),
                new OperationsSummaryResponse.Outbox(0, 1, 0, 0, 0),
                new OperationsSummaryResponse.Risk(0, 0, 0, 0),
                new OperationsSummaryResponse.MarketOrigin(1, 0, 0, 0),
                new OperationsSummaryResponse.Workforce(false, true, 100, 10, 0, 0, 8, 6, "NONE", "PRODUCTIVITY_REPORTED", "NONE"),
                new OperationsSummaryResponse.Runtime(true, true, "RUNNING",
                        LocalDateTime.parse("2026-07-10T10:00:00"),
                        LocalDateTime.parse("2026-07-10T10:00:00"),
                        3, 3, 1, "LIVE_WITH_BACKLOG"),
                new OperationsSummaryResponse.Ledger(false, "DISABLED", "http://localhost:18080", "/api/events/logistics/bulk", "LOGISTICS_CONFIRMED_NATIVE"),
                new OperationsSummaryResponse.Memory(512, 128)
        );

        assertThat(response.serviceName()).isEqualTo("Archive-Logistics");
        assertThat(response.serviceRole()).isEqualTo("Synthetic Logistics Event Backend");
        assertThat(response.liveFlowAvailable()).isTrue();
        assertThat(response.latestEventAt()).isEqualTo(LocalDateTime.parse("2026-07-10T10:00:00"));
        assertThat(response.shipmentsRequested()).isEqualTo(10);
        assertThat(response.workforce().driverCapacity()).isEqualTo(8);
        assertThat(response.runtime().schedulerStatus()).isEqualTo("RUNNING");
        assertThat(response.balance().operatingMargin()).isEqualByComparingTo("0.5");
        assertThat(response.balance().available()).isTrue();
        assertThat(response.balance().totalCost()).isEqualTo(50L);
    }
}
