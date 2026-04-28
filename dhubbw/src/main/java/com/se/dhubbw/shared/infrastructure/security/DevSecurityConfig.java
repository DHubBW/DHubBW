package com.se.dhubbw.shared.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@Profile("dev")
public class DevSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(withDefaults());
        return http.build();
    }
}
