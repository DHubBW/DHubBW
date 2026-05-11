package com.se.dhubbw.shared.infrastructure.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration
    .EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers
    .AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Development security configuration.
 *
 * <p>Configures security settings for the development profile, including
 * authorization rules, CSRF disabling, and in-memory user details.
 */
@Configuration
@EnableWebSecurity(debug = true)
@EnableMethodSecurity(securedEnabled = true)
@Profile("dev")
public class DevSecurityConfig {
    /**
     * Configure the security filter chain for development.
     *
     * <p>Permits access to actuator, student, and Swagger endpoints.
     * Disables CSRF and enables HTTP Basic authentication.
     *
     * @param http the {@code HttpSecurity} builder
     * @return the configured {@code SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http)
        throws Exception {
        http
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(withDefaults());
        return http.build();
    }

    /**
     * Configure password encoder for development.
     *
     * @return a {@code PasswordEncoder} using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure in-memory user details for development.
     * The username is meant to be the dhbw mail in a future release.
     *
     * <p>User roles are hierarchical:
     * - STUDENT: base role
     * - KURSADMIN: inherits STUDENT
     * - SYSTEMADMIN: inherits KURSADMIN and STUDENT
     *
     * @param encoder the {@code PasswordEncoder}
     * @return a {@code UserDetailsService} with multiple test users
     */
    @Bean
    public UserDetailsService userDetailsService(
        final PasswordEncoder encoder) {
        UserDetails student = User.withUsername("student")
            .password(encoder.encode("student"))
            .roles("STUDENT")
            .build();

        UserDetails admin = User.withUsername("kursadmin")
            .password(encoder.encode("kursadmin"))
            .roles("KURSADMIN", "STUDENT")
            .build();

        UserDetails sysAdmin = User.withUsername("systemadmin")
            .password(encoder.encode("systemadmin"))
            .roles("SYSTEMADMIN", "KURSADMIN", "STUDENT")
            .build();

        return new InMemoryUserDetailsManager(student, admin, sysAdmin);
    }
}
