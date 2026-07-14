package com.csj.archive.logistics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "archive.security")
public class ArchiveSecurityProperties {
    private boolean enabled;
    private String internalServiceToken = "";
    private String adminServiceToken = "";
    private String readToken = "";
    private Set<String> allowedSources = Set.of("archive-nexus", "archive-os");

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getInternalServiceToken() { return internalServiceToken; }
    public void setInternalServiceToken(String internalServiceToken) { this.internalServiceToken = internalServiceToken; }
    public String getAdminServiceToken() { return adminServiceToken; }
    public void setAdminServiceToken(String adminServiceToken) { this.adminServiceToken = adminServiceToken; }
    public String getReadToken() { return readToken; }
    public void setReadToken(String readToken) { this.readToken = readToken; }
    public Set<String> getAllowedSources() { return allowedSources; }
    public void setAllowedSources(Set<String> allowedSources) { this.allowedSources = allowedSources == null ? Set.of() : Set.copyOf(allowedSources); }
}
