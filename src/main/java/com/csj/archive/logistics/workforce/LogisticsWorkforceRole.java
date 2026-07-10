package com.csj.archive.logistics.workforce;

public enum LogisticsWorkforceRole {
    DISPATCH_PLANNER(20, 180_000L),
    ROUTE_PLANNER(30, 190_000L),
    DELIVERY_DRIVER(8, 220_000L),
    DELAY_RESPONSE_OPERATOR(10, 200_000L),
    COLD_CHAIN_HANDLER(12, 210_000L),
    LOGISTICS_MANAGER(50, 260_000L);

    private final int defaultCapacityPerPersonPerDay;
    private final long defaultWagePerDay;

    LogisticsWorkforceRole(int defaultCapacityPerPersonPerDay, long defaultWagePerDay) {
        this.defaultCapacityPerPersonPerDay = defaultCapacityPerPersonPerDay;
        this.defaultWagePerDay = defaultWagePerDay;
    }

    public int defaultCapacityPerPersonPerDay() {
        return defaultCapacityPerPersonPerDay;
    }

    public long defaultWagePerDay() {
        return defaultWagePerDay;
    }
}
