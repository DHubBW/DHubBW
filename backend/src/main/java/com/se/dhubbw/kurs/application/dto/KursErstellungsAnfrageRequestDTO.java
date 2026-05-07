package com.se.dhubbw.kurs.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body DTO for opening a new course creation request.
 *
 * <p>Carries the desired course name, cohort year, and optional
 * notes from the HTTP request to the service layer.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KursErstellungsAnfrageRequestDTO {

    /**
     * Desired name of the course to be created (e.g. "TINF24B2").
     * Must not be blank.
     */
    @NotBlank
    private String kursName;

    /**
     * Desired cohort year (e.g. "2024").
     * Must not be blank.
     */
    @NotBlank
    private String jahrgang;

    /**
     * Optional additional notes for the request.
     * May be {@code null} if no notes are provided.
     */
    private String additionalNotes;
}
