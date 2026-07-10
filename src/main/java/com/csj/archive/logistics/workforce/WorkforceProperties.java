package com.csj.archive.logistics.workforce;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "archive.workforce")
public class WorkforceProperties {
    private boolean enabled = false;
    private int baselineDispatchers = 2;
    private int baselineDrivers = 4;
    private int baselineDelayResponders = 1;
    private int dispatcherDailyCapacity = 25;
    private int driverDailyCapacity = 35;
    private int delayResponderDailyCapacity = 20;
    private long dispatcherDailyCost = 180_000L;
    private long driverDailyCost = 220_000L;
    private long delayResponderDailyCost = 200_000L;
    private int maxHop = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBaselineDispatchers() {
        return baselineDispatchers;
    }

    public void setBaselineDispatchers(int baselineDispatchers) {
        this.baselineDispatchers = baselineDispatchers;
    }

    public int getBaselineDrivers() {
        return baselineDrivers;
    }

    public void setBaselineDrivers(int baselineDrivers) {
        this.baselineDrivers = baselineDrivers;
    }

    public int getBaselineDelayResponders() {
        return baselineDelayResponders;
    }

    public void setBaselineDelayResponders(int baselineDelayResponders) {
        this.baselineDelayResponders = baselineDelayResponders;
    }

    public int getDispatcherDailyCapacity() {
        return dispatcherDailyCapacity;
    }

    public void setDispatcherDailyCapacity(int dispatcherDailyCapacity) {
        this.dispatcherDailyCapacity = dispatcherDailyCapacity;
    }

    public int getDriverDailyCapacity() {
        return driverDailyCapacity;
    }

    public void setDriverDailyCapacity(int driverDailyCapacity) {
        this.driverDailyCapacity = driverDailyCapacity;
    }

    public int getDelayResponderDailyCapacity() {
        return delayResponderDailyCapacity;
    }

    public void setDelayResponderDailyCapacity(int delayResponderDailyCapacity) {
        this.delayResponderDailyCapacity = delayResponderDailyCapacity;
    }

    public long getDispatcherDailyCost() {
        return dispatcherDailyCost;
    }

    public void setDispatcherDailyCost(long dispatcherDailyCost) {
        this.dispatcherDailyCost = dispatcherDailyCost;
    }

    public long getDriverDailyCost() {
        return driverDailyCost;
    }

    public void setDriverDailyCost(long driverDailyCost) {
        this.driverDailyCost = driverDailyCost;
    }

    public long getDelayResponderDailyCost() {
        return delayResponderDailyCost;
    }

    public void setDelayResponderDailyCost(long delayResponderDailyCost) {
        this.delayResponderDailyCost = delayResponderDailyCost;
    }

    public int getMaxHop() {
        return maxHop;
    }

    public void setMaxHop(int maxHop) {
        this.maxHop = maxHop;
    }
}
