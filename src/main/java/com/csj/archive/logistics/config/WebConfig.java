package com.csj.archive.logistics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebConfig {
    @Bean
    PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setFallbackPageable(PageRequest.of(0, 50));
            resolver.setMaxPageSize(500);
        };
    }
}
