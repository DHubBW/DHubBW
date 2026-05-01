package com.se.dhubbw.shared.infrastructure.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Production security configuration.
 *
 * <p>Placeholder configuration for the production profile.
 * Future enhancement: Enable JWT authentication for production security.
 */
@Configuration
@Profile("prod")
@EnableWebSecurity
public final class ProdSecurityConfiguration {
}
