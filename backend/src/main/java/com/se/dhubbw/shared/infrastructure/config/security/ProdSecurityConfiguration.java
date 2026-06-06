package com.se.dhubbw.shared.infrastructure.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration
    .AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.http.HttpStatus;

/**
 * Spring Security configuration for the production profile.
 * Configures JWT-based authentication, security headers, and CORS for
 * VM deployment.
 *
 * <p>Hardened for production VM deployment with same-origin frontend.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Security configuration)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true)
@Profile("prod")
public class ProdSecurityConfiguration {

    /**
     * BCrypt strength for production (higher = more secure, slower).
     */
    private static final int BCRYPT_STRENGTH = 12;

    /**
     * JWT authentication filter.
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * User details service.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Maximum header age.
     */
    private static final int MAX_AGE_SECONDS = 86400;

    /**
     * Constructor.
     *
     * @param filter the JWT filter
     * @param service      the user details service
     */
    public ProdSecurityConfiguration(
        final JwtAuthenticationFilter filter,
        final UserDetailsService service) {
        this.jwtAuthenticationFilter = filter;
        this.userDetailsService = service;
    }

    /**
     * Configure the security filter chain for production.
     * CORS is disabled (same-origin deployment on VM).
     * Security headers are enforced.
     *
     * @param http the {@code HttpSecurity} builder
     * @return the configured {@code SecurityFilterChain}
     */
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) {
        http
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(MAX_AGE_SECONDS))
                .contentTypeOptions(_ -> { })
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter
                        .HeaderValue.ENABLED_MODE_BLOCK))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter
                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; "
                        + "script-src 'self' 'unsafe-inline'; "
                        + "style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data:; "
                        + "font-src 'self'; "
                        + "connect-src 'self'; "
                        + "frame-ancestors 'none'")))
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/public/auth/register",
                    "/public/auth/login").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**",
                    "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Configure password encoder with increased BCrypt strength.
     *
     * @return a {@code PasswordEncoder} using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    /**
     * Configure authentication provider.
     *
     * @return the authentication provider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider =
            new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Configure authentication manager.
     *
     * @param config the authentication configuration
     * @return the authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(
        final AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }
}
