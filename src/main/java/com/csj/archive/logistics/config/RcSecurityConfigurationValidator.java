package com.csj.archive.logistics.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
@Profile("rc")
public class RcSecurityConfigurationValidator {
    private final Environment environment;

    public RcSecurityConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        List<String> required = List.of(
                "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD",
                "ARCHIVE_TOKEN_NEXUS_TO_LOGISTICS", "ARCHIVE_TOKEN_LOGISTICS_TO_LEDGER",
                "ARCHIVE_TOKEN_LOGISTICS_TO_OS", "ARCHIVE_TOKEN_AUTHENTICATED_READ", "ARCHIVE_TOKEN_ADMIN_OPERATOR"
        );
        for (String key : required) {
            String value = environment.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(key + " must be configured for the rc profile");
            }
        }
    }
}
