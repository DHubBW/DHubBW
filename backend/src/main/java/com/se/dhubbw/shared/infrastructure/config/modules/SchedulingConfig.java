package com.se.dhubbw.shared.infrastructure.config.modules;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class for enabling Spring Scheduling.
 * Enables the use of {@code @Scheduled} annotations for scheduled tasks.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring configuration)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
