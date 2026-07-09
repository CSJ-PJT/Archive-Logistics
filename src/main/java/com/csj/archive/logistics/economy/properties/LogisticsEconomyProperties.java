package com.csj.archive.logistics.economy.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "archive.economy")
public class LogisticsEconomyProperties {
    private boolean enabled = false;
    private boolean publishEnabled = true;
    private String defaultCurrency = "KRW";
    private long openingCashBalance = 5_000_000L;
    private int maxHop = 5;
    private BigDecimal dailySettlementFee = new BigDecimal("50000");
    private BigDecimal deliveryFeeMarginRate = new BigDecimal("0.0000");
    private long operationCostPerRoute = 3_000L;
    private long coldChainSurchargeRevenue = 40_000L;
    private long routeDeviationSurcharge = 60_000L;
    private long ledgerSettlementAgencyFeePerEvent = 12_000L;
    private long ledgerReconciliationFee = 8_000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPublishEnabled() {
        return publishEnabled;
    }

    public void setPublishEnabled(boolean publishEnabled) {
        this.publishEnabled = publishEnabled;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public long getOpeningCashBalance() {
        return openingCashBalance;
    }

    public void setOpeningCashBalance(long openingCashBalance) {
        this.openingCashBalance = openingCashBalance;
    }

    public int getMaxHop() {
        return maxHop;
    }

    public void setMaxHop(int maxHop) {
        this.maxHop = maxHop;
    }

    public BigDecimal getDailySettlementFee() {
        return dailySettlementFee;
    }

    public void setDailySettlementFee(BigDecimal dailySettlementFee) {
        this.dailySettlementFee = dailySettlementFee;
    }

    public BigDecimal getDeliveryFeeMarginRate() {
        return deliveryFeeMarginRate;
    }

    public void setDeliveryFeeMarginRate(BigDecimal deliveryFeeMarginRate) {
        this.deliveryFeeMarginRate = deliveryFeeMarginRate;
    }

    public long getOperationCostPerRoute() {
        return operationCostPerRoute;
    }

    public void setOperationCostPerRoute(long operationCostPerRoute) {
        this.operationCostPerRoute = operationCostPerRoute;
    }

    public long getColdChainSurchargeRevenue() {
        return coldChainSurchargeRevenue;
    }

    public void setColdChainSurchargeRevenue(long coldChainSurchargeRevenue) {
        this.coldChainSurchargeRevenue = coldChainSurchargeRevenue;
    }

    public long getRouteDeviationSurcharge() {
        return routeDeviationSurcharge;
    }

    public void setRouteDeviationSurcharge(long routeDeviationSurcharge) {
        this.routeDeviationSurcharge = routeDeviationSurcharge;
    }

    public long getLedgerSettlementAgencyFeePerEvent() {
        return ledgerSettlementAgencyFeePerEvent;
    }

    public void setLedgerSettlementAgencyFeePerEvent(long ledgerSettlementAgencyFeePerEvent) {
        this.ledgerSettlementAgencyFeePerEvent = ledgerSettlementAgencyFeePerEvent;
    }

    public long getLedgerReconciliationFee() {
        return ledgerReconciliationFee;
    }

    public void setLedgerReconciliationFee(long ledgerReconciliationFee) {
        this.ledgerReconciliationFee = ledgerReconciliationFee;
    }
}
