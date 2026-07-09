package com.csj.archive.logistics.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder().requestFactory(new JdkClientHttpRequestFactory());
    }

    @Bean
    RestClientCustomizer archiveRestClientCustomizer() {
        return builder -> builder.defaultHeader("User-Agent", "Archive-Logitics/1.0");
    }
}
