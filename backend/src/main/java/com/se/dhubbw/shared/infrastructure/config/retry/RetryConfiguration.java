package com.se.dhubbw.shared.infrastructure.config.retry;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enables Spring Retry support application-wide.
 *
 * <p>Activates {@code @Retryable} and {@code @Recover} annotations
 * for handling transient failures such as database deadlocks.</p>
 */
@Configuration
@EnableRetry
public class RetryConfiguration {
}
