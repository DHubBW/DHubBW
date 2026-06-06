package com.se.dhubbw.shared.infrastructure.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Color-coded logger wrapper for console output.
 * Provides methods for different log levels with ANSI color codes.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (SLF4J logger wrapper)
 */
public final class ColorLogger {
    /**
     * The underlying SLF4J logger.
     */
    private final Logger logger;

    /**
     * Construct a ColorLogger for the given class.
     *
     * @param tClass the class to create logger for
     * @param <T>    the type parameter
     */
    public <T> ColorLogger(final Class<T> tClass) {
        logger = LoggerFactory.getLogger(tClass);
    }

    /**
     * Log a debug message with color.
     *
     * @param msg       the message format
     * @param arguments the message arguments
     */
    public void colorDebug(final String msg, final Object... arguments) {
        String colorMsg = "[34m" + msg + "[0m";
        logger.debug(colorMsg, arguments);
    }

    /**
     * Log an error message with color.
     *
     * @param msg       the message format
     * @param arguments the message arguments
     */
    public void colorError(final String msg, final Object... arguments) {
        String colorMsg = "[31m" + msg + "[0m";
        logger.error(colorMsg, arguments);
    }

    /**
     * Log an info message with color.
     *
     * @param msg       the message format
     * @param arguments the message arguments
     */
    public void colorInfo(final String msg, final Object... arguments) {
        String colorMsg = "[32m" + msg + "[0m";
        logger.info(colorMsg, arguments);
    }

    /**
     * Log a warning message with orange color.
     *
     * @param msg       the message format
     * @param arguments the message arguments
     */
    public void colorNewAdmin(final String msg, final Object... arguments) {
        String colorMsg = "[33m" + msg + "[0m";
        logger.warn(colorMsg, arguments);
    }

    /**
     * Log a warning message with yellow color (warn level).
     *
     * @param msg       the message format
     * @param arguments the message arguments
     */
    public void colorWarn(final String msg, final Object... arguments) {
        String colorMsg = "[33m" + msg + "[0m";
        logger.warn(colorMsg, arguments);
    }

    /**
     * Log a critical error message with magenta color (error level).
     *
     * <p>Use for unrecoverable errors and unexpected server-side
     * failures that require immediate attention.</p>
     *
     * @param msg       the message format
     * @param arguments the message arguments
     */
    public void colorCritical(final String msg, final Object... arguments) {
        String colorMsg = "[35m" + msg + "[0m";
        logger.error(colorMsg, arguments);
    }
}
