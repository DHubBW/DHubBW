package com.se.dhubbw.shared.infrastructure.config.retry;

import com.se.dhubbw.shared.infrastructure.constants.Constants;
import com.se.dhubbw.shared.testing.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UnitTest
public class RetryableTransactionalTest {

    @Test
    public void annotationTargetIsMethod() {
        Target target = RetryableTransactional.class
            .getAnnotation(Target.class);
        assertNotNull(target);
        assertEquals(1, target.value().length);
        assertEquals(ElementType.METHOD, target.value()[0]);
    }

    @Test
    public void annotationRetentionIsRuntime() {
        Retention retention = RetryableTransactional.class
            .getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void annotationIsMetaAnnotatedWithTransactional() {
        assertNotNull(RetryableTransactional.class
            .getAnnotation(Transactional.class));
    }

    @Test
    public void retryableMaxAttemptsMatchesConstant() {
        Retryable retryable = RetryableTransactional.class
            .getAnnotation(Retryable.class);
        assertNotNull(retryable);
        assertEquals(Constants.RETRY_MAX_ATTEMPTS, retryable.maxAttempts());
    }

    @Test
    public void retryableBackoffDelayMatchesConstant() {
        Retryable retryable = RetryableTransactional.class
            .getAnnotation(Retryable.class);
        assertNotNull(retryable);
        Backoff backoff = retryable.backoff();
        assertEquals(Constants.RETRY_DELAY_MS, backoff.delay());
        assertTrue(backoff.random());
    }

    @Test
    public void retryableFiresOnCannotAcquireLockException() {
        Retryable retryable = RetryableTransactional.class
            .getAnnotation(Retryable.class);
        assertNotNull(retryable);
        assertTrue(
            Arrays.asList(retryable.retryFor())
                .contains(CannotAcquireLockException.class));
    }

    @Test
    public void retryableFiresOnDeadlockLoserDataAccessException() {
        Retryable retryable = RetryableTransactional.class
            .getAnnotation(Retryable.class);
        assertNotNull(retryable);
        assertTrue(
            Arrays.asList(retryable.retryFor())
                .contains(DeadlockLoserDataAccessException.class));
    }
}
