package com.se.dhubbw.shared.infrastructure.aspects.logging;

import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AOP aspect for logging service method calls.
 *
 * <p>Logs entry, successful return, and exceptions for all methods
 * in classes annotated with {@code @Service} or ending with "ServiceImpl".</p>
 */
@Aspect
@Component
public class ServiceLoggingAspect {

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
     * Pointcut for service methods.
     * Matches all methods in classes annotated with @Service
     * or classes ending with "ServiceImpl".
     */
    @Pointcut("within(@org.springframework.stereotype.Service *) "
        + "|| within(*..ServiceImpl)")
    public void serviceMethods() {
        // Pointcut definition
    }

    /**
     * Log method call before execution.
     *
     * @param joinPoint the join point
     */
    @Before("serviceMethods()")
    public void logBefore(final JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        colorLogger.colorInfo(
            "[{}] → {}.{}() called",
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            className,
            methodName);
    }

    /**
     * Log successful method return.
     *
     * @param joinPoint the join point
     * @param result the return value
     */
    @AfterReturning(pointcut = "serviceMethods()", returning = "result")
    public void logAfterReturning(final JoinPoint joinPoint,
                                   final Object result) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        if (result != null) {
            colorLogger.colorInfo(
                "[{}] ✓ {}.{}() completed successfully",
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                className,
                methodName);
        } else {
            colorLogger.colorInfo(
                "[{}] ✓ {}.{}() completed (void)",
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                className,
                methodName);
        }
    }

    /**
     * Log exceptions thrown by service methods.
     *
     * @param joinPoint the join point
     * @param exception the thrown exception
     */
    @AfterThrowing(pointcut = "serviceMethods()", throwing = "exception")
    public void logAfterThrowing(final JoinPoint joinPoint,
                                  final Exception exception) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        colorLogger.colorError(
            "[{}] ✗ {}.{}() threw {}: {}",
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            className,
            methodName,
            exception.getClass().getSimpleName(),
            exception.getMessage());
    }
}
