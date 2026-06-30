package com.se.dhubbw.kurs.application.dto.input;

import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object for the request body of a course creation
 * request.
 * Carries course name, cohort year, and optional notes from the client
 * to the service.
 *
 * <p>Carries the desired course name, cohort year, and optional
 * notes from the HTTP request to the service layer.</p>
 *
 * @param kursName        desired name of the course
 * to be created (e.g. "TINF24B2"). Must not be blank.
 * @param jahrgang        desired cohort year (e.g. "2024"). Must not be blank.
 * @param additionalNotes optional additional notes for the request
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation HTTP
 *     Request → Service)
 */
public record KursErstellungsAnfrageRequestDTO(
    @NotBlank String kursName,
    @NotBlank String jahrgang,
    String additionalNotes) { }
