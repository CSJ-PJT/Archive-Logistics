package com.csj.archive.logistics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI archiveLogiticsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Archive-Logitics API")
                        .version("1.0.0")
                        .description("Synthetic logistics event backend for Archive Platform Ecosystem"));
    }
}
