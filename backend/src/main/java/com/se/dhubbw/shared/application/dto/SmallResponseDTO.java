package com.se.dhubbw.shared.application.dto;

/**
 * Minimal data transfer object for simple text responses.
 * Used when only a string message needs to be returned to the client.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation for
 *     API Response)
 * @param response The text response for the user.
 */
public record SmallResponseDTO(String response) { }
