package com.csj.archive.logistics.ledger;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "archive.ledger")
public class LedgerPublishProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:18080";
    private String bulkEndpoint = "/api/events/logistics/bulk";
    private int publishTimeoutMs = 3000;
    private LedgerContractMode contractMode = LedgerContractMode.LOGISTICS_CONFIRMED_NATIVE;

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

    public String getBulkEndpoint() {
        return bulkEndpoint;
    }

    public void setBulkEndpoint(String bulkEndpoint) {
        this.bulkEndpoint = bulkEndpoint;
    }

    public int getPublishTimeoutMs() {
        return publishTimeoutMs;
    }

    public void setPublishTimeoutMs(int publishTimeoutMs) {
        this.publishTimeoutMs = publishTimeoutMs;
    }

    public LedgerContractMode getContractMode() {
        return contractMode;
    }

    public void setContractMode(LedgerContractMode contractMode) {
        this.contractMode = contractMode;
    }

    public String endpoint() {
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        String endpoint = bulkEndpoint == null || bulkEndpoint.isBlank() ? "/api/events/logistics/bulk" : bulkEndpoint;
        return endpoint.startsWith("/") ? base + endpoint : base + "/" + endpoint;
    }
}
