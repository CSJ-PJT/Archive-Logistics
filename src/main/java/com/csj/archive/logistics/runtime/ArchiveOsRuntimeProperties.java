package com.csj.archive.logistics.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "archive.archiveos.runtime-ingest")
public class ArchiveOsRuntimeProperties {
    private boolean enabled;
    private String baseUrl = "";
    private String path = "/api/live-flow/events/ingest";
    private String token = "";
    private boolean schedulerEnabled;
    private int maxRetryCount = 5;
    private int batchSize = 25;
    private int stalePublishingSeconds = 300;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public void setSchedulerEnabled(boolean schedulerEnabled) { this.schedulerEnabled = schedulerEnabled; }
    public int getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(int maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getStalePublishingSeconds() { return stalePublishingSeconds; }
    public void setStalePublishingSeconds(int stalePublishingSeconds) { this.stalePublishingSeconds = stalePublishingSeconds; }
}
