package com.se.dhubbw.kurs.application.dto.output;

import java.util.UUID;

/**
 * Data transfer object for representing a course in API responses.
 * Contains identifier, name, and cohort year for the system admin overview.
 *
 * <p>Used by system-admin endpoints to expose all courses
 * with their identifier, name, and cohort year.</p>
 *
 * @param kursID   unique identifier of the course
 * @param kursName name of the course (e.g. "TINF24B2")
 * @param jahrgang cohort year of the course (e.g. "2024")
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
public record KursDTO(UUID kursID, String kursName, String jahrgang) { }
