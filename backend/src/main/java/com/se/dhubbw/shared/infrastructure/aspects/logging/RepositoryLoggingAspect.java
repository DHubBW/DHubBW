package com.se.dhubbw.shared.infrastructure.aspects.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for logging repository method invocations.
 * Logs database accesses at debug level for all repository interfaces.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring AOP aspect)
 */
@Aspect
@Component
public class RepositoryLoggingAspect {

    /**
     * Logger instance for colored output.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(this.getClass());

    /**
     * Timestamp format for logging.
     */
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Pointcut for repository methods.
     */
    @Pointcut("execution(* com.se.dhubbw..*Repository+.*(..))")
    public void repositoryMethods() {
        // Pointcut definition
    }

    /**
     * Log method call before execution.
     *
     * @param joinPoint the join point
     */
    @Before("repositoryMethods()")
    public void logBefore(final JoinPoint joinPoint) {
        colorLogger.colorDebug(
            "[{}] BEFORE - method '{}' was called",
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            joinPoint.getSignature().toShortString());
    }

    /**
     * Log method call after execution.
     *
     * @param joinPoint the join point
     */
    @After("repositoryMethods()")
    public void logAfter(final JoinPoint joinPoint) {
        colorLogger.colorDebug(
            "[{}] AFTER - method '{}' was called",
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            joinPoint.getSignature().toShortString());
    }
}

