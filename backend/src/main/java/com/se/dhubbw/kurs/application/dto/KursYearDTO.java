package com.se.dhubbw.kurs.application.dto;

import java.util.UUID;

/**
 * Data Transfer Object for course name and year information.
 *
 * <p>Used by {@code retrieveKursNamesAndYears} to expose all available
 * courses with their unique identifier, name, and cohort year.</p>
 * @param kursID The ID of the kurs.
 * @param kursName The name of the kurs.
 * @param jahrgang the year of the kurs.
 */
public record KursYearDTO(UUID kursID,
                          String kursName,
                          String jahrgang) {
}
