package com.csj.archive.logistics.settlement;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "archive.nexus-settlement")
public class NexusSettlementProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:8080";
    private String dailyEndpoint = "/api/logistics/settlements/daily";
    private int publishTimeoutMs = 30000;
    private int maxRetryCount = 5;
    private BigDecimal manufacturingShareRate = new BigDecimal("0.3000");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDailyEndpoint() {
        return dailyEndpoint;
    }

    public void setDailyEndpoint(String dailyEndpoint) {
        this.dailyEndpoint = dailyEndpoint;
    }

    public int getPublishTimeoutMs() {
        return publishTimeoutMs;
    }

    public void setPublishTimeoutMs(int publishTimeoutMs) {
        this.publishTimeoutMs = publishTimeoutMs;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public BigDecimal getManufacturingShareRate() {
        return manufacturingShareRate;
    }

    public void setManufacturingShareRate(BigDecimal manufacturingShareRate) {
        this.manufacturingShareRate = manufacturingShareRate;
    }

    public String endpoint() {
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        String endpoint = dailyEndpoint == null || dailyEndpoint.isBlank()
                ? "/api/logistics/settlements/daily"
                : dailyEndpoint;
        return endpoint.startsWith("/") ? base + endpoint : base + "/" + endpoint;
    }
}
