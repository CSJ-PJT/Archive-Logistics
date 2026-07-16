package com.csj.archive.logistics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final ArchiveRequestSecurityInterceptor archiveRequestSecurityInterceptor;

    public WebConfig(ArchiveRequestSecurityInterceptor archiveRequestSecurityInterceptor) {
        this.archiveRequestSecurityInterceptor = archiveRequestSecurityInterceptor;
    }

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setFallbackPageable(PageRequest.of(0, 50));
            resolver.setMaxPageSize(500);
        };
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(archiveRequestSecurityInterceptor).addPathPatterns("/api/**", "/actuator/**");
    }
}
