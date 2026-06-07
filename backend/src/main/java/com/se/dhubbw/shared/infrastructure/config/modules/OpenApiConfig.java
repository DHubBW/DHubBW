package com.se.dhubbw.shared.infrastructure.config.modules;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Registers the JWT Bearer security scheme and API metadata for Swagger UI.
 *
 * <p>Configures the OpenAPI specification for the DHubBW Backend API,
 * enabling interactive API documentation through Swagger UI.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring configuration with OpenAPI)
 */
@Profile({"dev", "prod"})
@Configuration
public class OpenApiConfig {

    /**
     * Base URL des API-Servers (ohne trailing Slash).
     * Dev: leer (relative Pfade), Prod: https://vm553.ipp-vps.net/api
     */
    @Value("${openapi.server-url:}")
    private String serverUrl;

    /**
     * Create custom OpenAPI specification with JWT Bearer
     * token security scheme.
     *
     * <p>Configures Swagger UI to send the JWT via the
     * {@code Authorization: Bearer} header, matching the
     * fallback path in {@link
     * com.se.dhubbw.shared.infrastructure.config.security
     * .JwtAuthenticationFilter}.</p>
     *
     * @return the configured {@code OpenAPI} instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
            .info(new Info()
                .title("DHubBW API")
                .version("0.0.1-SNAPSHOT")
                .description(
                    "REST API Dokumentation für das DHubBW Backend"))
            .addSecurityItem(new SecurityRequirement()
                .addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .name("bearerAuth")));
        if (serverUrl != null && !serverUrl.isBlank()) {
            openAPI.addServersItem(new Server().url(serverUrl));
        }
        return openAPI;
    }
}
