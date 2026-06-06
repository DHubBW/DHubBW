package com.se.dhubbw.shared.infrastructure.aspects.logging;

import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AOP aspect for logging REST controller method invocations.
 * Logs HTTP requests, responses, and exceptions for all RestController classes.
 *
 * <p>Logs HTTP requests, responses, and exceptions for all methods
 * in classes annotated with {@code @RestController}.</p>
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring AOP aspect)
 */
@Aspect
@Component
public class ControllerLoggingAspect {

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
     * Pointcut for REST controller methods.
     * Matches all methods in classes annotated with @RestController.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation"
        + ".RestController *)")
    public void controllerMethods() {
        // Pointcut definition
    }

    /**
     * Log HTTP request before execution.
     *
     * @param joinPoint the join point
     */
    @Before("controllerMethods()")
    public void logBefore(final JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        StringBuilder argsLog = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null
                && !args[i].getClass().getName()
                    .startsWith("org.springframework.security")
                && !args[i].getClass().getName()
                    .startsWith("jakarta.servlet")) {
                argsLog.append(args[i].getClass().getSimpleName());
                if (i < args.length - 1) {
                    argsLog.append(", ");
                }
            }
        }

        colorLogger.colorInfo(
            "[{}] HTTP → {}.{}({})",
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            className,
            methodName,
            argsLog.toString());
    }

    /**
     * Log HTTP response after successful execution.
     *
     * @param joinPoint the join point
     * @param result the return value
     */
    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void logAfterReturning(final JoinPoint joinPoint,
                                   final Object result) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        if (result instanceof ResponseEntity<?> responseEntity) {
            colorLogger.colorInfo(
                "[{}] HTTP ← {}.{}() → {} {}",
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                className,
                methodName,
                responseEntity.getStatusCode().value(),
                responseEntity.getStatusCode());
        } else {
            colorLogger.colorInfo(
                "[{}] HTTP ← {}.{}() completed",
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                className,
                methodName);
        }
    }

    /**
     * Log exceptions thrown by controller methods.
     *
     * @param joinPoint the join point
     * @param exception the thrown exception
     */
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "exception")
    public void logAfterThrowing(final JoinPoint joinPoint,
                                  final Exception exception) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        colorLogger.colorError(
            "[{}] HTTP ✗ {}.{}() threw {}: {}",
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            className,
            methodName,
            exception.getClass().getSimpleName(),
            exception.getMessage());
    }
}
