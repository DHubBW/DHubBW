package com.se.dhubbw.shared.infrastructure.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Color-based logger wrapper for console output.
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
        String colorMsg = "\u001B[34m" + msg + "\u001B[0m";
        logger.debug(colorMsg, arguments);
    }

    /**
     * Log an error message with color.
     *
     * @param msg       the message format
     * @param arguments the message arguments
     */
    public void colorError(final String msg, final Object... arguments) {
        String colorMsg = "\u001B[31m" + msg + "\u001B[0m";
        logger.error(colorMsg, arguments);
    }

    /**
     * Log an info message with color.
     *
     * @param msg       the message format
     * @param arguments the message arguments
     */
    public void colorInfo(final String msg, final Object... arguments) {
        String colorMsg = "\u001B[32m" + msg + "\u001B[0m";
        logger.info(colorMsg, arguments);
    }
}
