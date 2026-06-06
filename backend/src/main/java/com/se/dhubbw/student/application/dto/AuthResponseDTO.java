package com.se.dhubbw.student.application.dto;

/**
 * Data transfer object for authentication responses (login/registration).
 * Contains result message, JWT token, and expiration timestamp.
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation for
 *     API Response)
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
