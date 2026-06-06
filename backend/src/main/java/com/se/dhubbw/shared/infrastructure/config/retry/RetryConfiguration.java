package com.se.dhubbw.shared.infrastructure.config.retry;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration class for enabling Spring Retry.
 * Enables {@code @Retryable} and {@code @Recover} annotations for
 * handling transient failures.
 *
 * <p>Activates {@code @Retryable} and {@code @Recover} annotations
 * for handling transient failures such as database deadlocks.</p>
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring configuration)
 */
@Configuration
@EnableRetry
public class RetryConfiguration {
}
