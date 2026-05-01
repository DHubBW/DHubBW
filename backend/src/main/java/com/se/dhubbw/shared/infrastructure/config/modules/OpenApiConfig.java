package com.se.dhubbw.shared.infrastructure.config.modules;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI configuration for Swagger documentation.
 *
 * <p>Configures the OpenAPI specification for the DHubBW Backend API,
 * enabling interactive API documentation through Swagger UI.
 */
@Profile("dev")
@Configuration
public class OpenApiConfig {

    /**
     * Create custom OpenAPI specification.
     *
     * @return the configured {@code OpenAPI} instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("DHubBW API")
                .version("0.0.1-SNAPSHOT")
                .description(
                    "REST API Dokumentation für das DHubBW Backend"));
    }
}
