package com.se.dhubbw.shared.infrastructure.constants;

/**
 * Application-wide constants shared across all bounded contexts.
 * Contains retry parameters and other technical configuration values.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (technical configuration constants)
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
     * Minimum length required for a new password.
     */
    public static final int PASSWORD_MIN_LENGTH = 10;

    /**
     * Maximum length allowed for a new password.
     */
    public static final int PASSWORD_MAX_LENGTH = 128;

    /**
     * Regex describing the project's password policy.
     *
     * <p>Requirements: at least one lowercase letter, one uppercase
     * letter, one digit, one special character (non-letter, non-digit,
     * non-whitespace), {@link #PASSWORD_MIN_LENGTH}..
     * {@link #PASSWORD_MAX_LENGTH} non-whitespace characters total.</p>
     */
    public static final String PASSWORD_REGEX =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])"
            + "(?=.*[^A-Za-z0-9\\s])\\S{"
            + PASSWORD_MIN_LENGTH + "," + PASSWORD_MAX_LENGTH + "}$";

    /**
     * Private constructor to prevent instantiation.
     */
    private Constants() { }
}
