package com.se.dhubbw.student.application.dto;

/**
 * Response DTO for authentication endpoints (login and register).
 *
 * @param message   human-readable result message
 * @param token     JWT token for use in Authorization: Bearer headers
 * @param expiresAt Unix timestamp in milliseconds when the token expires
 */
public record AuthResponseDTO(
    String message,
    String token,
    long expiresAt
) {
}
