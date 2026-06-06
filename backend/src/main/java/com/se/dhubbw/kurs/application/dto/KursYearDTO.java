package com.se.dhubbw.kurs.application.dto;

import java.util.UUID;

/**
 * Data transfer object for course name and cohort year.
 * Used in the course listing for students.
 *
 * <p>Used by {@code retrieveKursNamesAndYears} to expose all available
 * courses with their unique identifier, name, and cohort year.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 * @param kursID The ID of the kurs.
 * @param kursName The name of the kurs.
 * @param jahrgang the year of the kurs.
 */
public record KursYearDTO(UUID kursID,
                          String kursName,
                          String jahrgang) {
}
