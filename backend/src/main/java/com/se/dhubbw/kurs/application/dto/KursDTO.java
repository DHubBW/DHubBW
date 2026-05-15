package com.se.dhubbw.kurs.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing a course.
 *
 * <p>Used by system-admin endpoints to expose all courses
 * with their identifier, name, and cohort year.</p>
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
