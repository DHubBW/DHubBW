package com.se.dhubbw.shared.infrastructure.constants;

/**
 * Application-wide constants shared across bounded contexts.
 */
public final class Constants {

    /**
     * Maximum retry attempts on database deadlock or lock
     * acquisition failure.
     */
    public static final int RETRY_MAX_ATTEMPTS = 3;

    /**
     * Initial back-off delay in milliseconds for retry attempts.
     */
    public static final long RETRY_DELAY_MS = 200L;

    /**
     * Private constructor to prevent instantiation.
     */
    private Constants() { }
}
