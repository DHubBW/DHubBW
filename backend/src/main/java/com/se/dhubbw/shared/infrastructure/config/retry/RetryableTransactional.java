package com.se.dhubbw.shared.infrastructure.config.retry;

import com.se.dhubbw.shared.infrastructure.constants.Constants;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composed annotation combining {@link Transactional} with
 * deadlock-resilient {@link Retryable} retry behaviour.
 *
 * <p>Applies a write transaction that retries up to
 * {@link Constants#RETRY_MAX_ATTEMPTS} times on
 * {@link CannotAcquireLockException} or
 * {@link DeadlockLoserDataAccessException}, with an exponential
 * back-off starting at {@link Constants#RETRY_DELAY_MS} ms.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
        retryFor = {CannotAcquireLockException.class,
            DeadlockLoserDataAccessException.class},
        maxAttempts = Constants.RETRY_MAX_ATTEMPTS,
        backoff = @Backoff(
            delay = Constants.RETRY_DELAY_MS, multiplier = 2.0,
            random = true))
@Transactional
public @interface RetryableTransactional {
}
