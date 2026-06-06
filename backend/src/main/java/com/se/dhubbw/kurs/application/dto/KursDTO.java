package com.se.dhubbw.kurs.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data transfer object for representing a course in API responses.
 * Contains identifier, name, and cohort year for the system admin overview.
 *
 * <p>Used by system-admin endpoints to expose all courses
 * with their identifier, name, and cohort year.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KursDTO {

    /**
     * Unique identifier of the course.
     */
    private UUID kursID;

    /**
     * Name of the course (e.g. "TINF24B2").
     */
    private String kursName;

    /**
     * Cohort year of the course (e.g. "2024").
     */
    private String jahrgang;
}
