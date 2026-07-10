package com.csj.archive.logistics.workforce;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkforceControllerTest {
    private final WorkforceService workforceService = mock(WorkforceService.class);
    private final WorkforceController controller = new WorkforceController(workforceService);

    @Test
    void workforceSummaryReturnsDefaultData() {
        when(workforceService.workforceSummary()).thenReturn(workforceSummary());

        var response = controller.workforceSummary();

        assertThat(response.data().available()).isTrue();
        assertThat(response.data().backlogCount()).isZero();
        assertThat(response.data().bottleneckRole()).isEqualTo("NONE");
    }

    @Test
    void productivitySummaryReturnsDefaultData() {
        when(workforceService.productivitySummary()).thenReturn(new ProductivitySummaryResponse(
                "Archive-Logistics",
                LocalDate.parse("2026-07-10"),
                0,
                0,
                BigDecimal.ZERO.setScale(4),
                BigDecimal.ZERO.setScale(4),
                0,
                "PRODUCTIVITY_REPORTED"
        ));

        var response = controller.productivitySummary();

        assertThat(response.data().processedEvents()).isZero();
        assertThat(response.data().status()).isEqualTo("PRODUCTIVITY_REPORTED");
    }

    @Test
    void capacitySummaryReturnsDefaultData() {
        when(workforceService.capacitySummary()).thenReturn(new CapacitySummaryResponse(
                "Archive-Logistics",
                LocalDate.parse("2026-07-10"),
                false,
                true,
                0,
                0,
                0,
                0,
                0,
                0,
                "NONE"
        ));

        var response = controller.capacitySummary();

        assertThat(response.data().backlogEvents()).isZero();
        assertThat(response.data().bottleneckType()).isEqualTo("NONE");
    }

    private WorkforceSummaryResponse workforceSummary() {
        return new WorkforceSummaryResponse(
                "Archive-Logistics",
                "Archive-Logistics",
                true,
                false,
                true,
                LocalDate.parse("2026-07-10"),
                "WORKDAY-20260710-TEST",
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                "NONE",
                BigDecimal.ZERO.setScale(4),
                0,
                LocalDateTime.parse("2026-07-10T00:00:00"),
                null,
                "PRODUCTIVITY_REPORTED",
                "NONE"
        );
    }
}
