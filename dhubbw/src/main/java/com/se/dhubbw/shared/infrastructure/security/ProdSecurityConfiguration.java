package com.se.dhubbw.shared.infrastructure.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@Profile("prod")
@EnableWebSecurity
// TODO: Enhance the prod SecurityConfiguration to enable jwt authentication
public class ProdSecurityConfiguration {
}
