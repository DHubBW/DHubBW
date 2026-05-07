package com.se.dhubbw.kurs.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for course name and year information.
 *
 * <p>Used by {@code retrieveKursNamesAndYears} to expose all available
 * courses with their unique identifier, name, and cohort year.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KursYearDTO {

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
