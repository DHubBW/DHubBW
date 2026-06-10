package com.se.dhubbw.shared.infrastructure.exception;

import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Global exception handler for all REST controllers.
 * Centralises HTTP error responses and maps exceptions to standard HTTP
 * status codes.
 *
 * <p>Centralises HTTP error responses to avoid duplicated
 * try-catch blocks in individual controllers. The handler maps
 * common exception types to standard HTTP status codes and logs
 * every error with the appropriate colour.</p>
 *
 * <p>Exception mapping overview:</p>
 * <ul>
 *   <li>{@link ResponseStatusException} – propagates the status and
 *       reason set by the throwing service</li>
 *   <li>{@link NoSuchElementException} – 404 Not Found</li>
 *   <li>{@link IllegalStateException} – 409 Conflict</li>
 *   <li>{@link IllegalArgumentException} – 400 Bad Request</li>
 *   <li>{@link MethodArgumentNotValidException} – 400 Bad Request
 *       with field-level validation details</li>
 *   <li>{@link MissingServletRequestParameterException} –
 *       400 Bad Request for missing required parameters</li>
 *   <li>{@link AccessDeniedException} – 403 Forbidden</li>
 *   <li>{@link Exception} – 500 Internal Server Error (fallback)</li>
 * </ul>
 *
 * <p>Controllers that need a different mapping for a specific
 * exception type (e.g. {@code IllegalArgumentException} → 403)
 * can still use a local {@code try-catch} block; local handlers
 * always take precedence over this advice.</p>
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring RestControllerAdvice)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * HTTP 413 Payload Too Large status value. Hard-coded as an
     * integer because {@link HttpStatus#PAYLOAD_TOO_LARGE} is
     * deprecated in Spring 7 and would trigger a build warning.
     */
    private static final int HTTP_PAYLOAD_TOO_LARGE = 413;

    /**
     * ColorLogger for error output.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(GlobalExceptionHandler.class);

    /**
     * Propagate a {@link ResponseStatusException} thrown by a service.
     *
     * @param ex the exception carrying the desired HTTP status
     * @return a response with the exception's status and reason phrase
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(
            final ResponseStatusException ex) {
        colorLogger.colorWarn(
            "ResponseStatusException {}: {}",
            ex.getStatusCode().value(), ex.getReason());
        return ResponseEntity
            .status(ex.getStatusCode())
            .body(ex.getReason());
    }

    /**
     * Map a {@link NoSuchElementException} to 404 Not Found.
     *
     * @param ex the exception
     * @return 404 response with the exception message
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElementException(
            final NoSuchElementException ex) {
        colorLogger.colorWarn(
            "Resource not found: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ex.getMessage());
    }

    /**
     * Map an {@link IllegalStateException} to 409 Conflict.
     *
     * @param ex the exception
     * @return 409 response with the exception message
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(
            final IllegalStateException ex) {
        colorLogger.colorWarn(
            "Conflict: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ex.getMessage());
    }

    /**
     * Map an {@link IllegalArgumentException} to 400 Bad Request.
     *
     * <p>Controllers that need a different mapping (e.g. 403 Forbidden)
     * must catch {@code IllegalArgumentException} locally.</p>
     *
     * @param ex the exception
     * @return 400 response with the exception message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(
            final IllegalArgumentException ex) {
        colorLogger.colorWarn(
            "Bad request: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ex.getMessage());
    }

    /**
     * Map a {@link MethodArgumentNotValidException} to 400 Bad Request
     * with a summary of all field-level validation errors.
     *
     * @param ex the validation exception
     * @return 400 response listing every failing field and its message
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<String>>
            handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .toList();
        colorLogger.colorWarn(
            "Validation failed with {} error(s): {}",
            errors.size(), errors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errors);
    }

    /**
     * Map a {@link MissingServletRequestParameterException} to 400.
     *
     * @param ex the exception for the missing parameter
     * @return 400 response with the exception message
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingRequestParam(
            final MissingServletRequestParameterException ex) {
        colorLogger.colorWarn(
            "Missing request parameter: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ex.getMessage());
    }

    /**
     * Map an {@link AccessDeniedException} to 403 Forbidden.
     *
     * <p>Handles Spring Security authorization failures from
     * {@code @PreAuthorize} annotations.</p>
     *
     * @param ex the access denied exception
     * @return 403 response with the exception message
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(
            final AccessDeniedException ex) {
        colorLogger.colorWarn(
            "Access denied: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ex.getMessage());
    }

    /**
     * Map Spring's multipart resolver overflow to 413 Payload Too Large.
     *
     * <p>Without this handler the default Spring MVC behaviour wraps
     * the cause in a 500 response, which the frontend cannot
     * distinguish from a real server error. The 413 status maps
     * cleanly to the {@code 'too-large'} upload-error code in
     * {@code useFileUpload}.</p>
     *
     * @param ex the exception thrown by the multipart resolver
     * @return 413 response with a German operator hint
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxUploadSizeExceeded(
            final MaxUploadSizeExceededException ex) {
        colorLogger.colorWarn(
            "Upload rejected — file exceeds the configured limit: {}",
            ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.valueOf(HTTP_PAYLOAD_TOO_LARGE))
            .body("Datei ist zu groß. Maximal 50 MB pro Upload.");
    }

    /**
     * Fallback handler for all unhandled exceptions (500 Internal
     * Server Error).
     *
     * @param ex the unhandled exception
     * @return 500 response with a generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(
            final Exception ex) {
        colorLogger.colorCritical(
            "Unhandled exception {}: {}",
            ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("An unexpected server error occurred.");
    }
}
