package com.se.dhubbw.kurs.application.dto.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data transfer object for a course creation request (system admin view).
 * Contains the data a system admin needs to review a creation request.
 *
 * <p>Carries the data a system administrator needs to review
 * an open course creation request: the request identifier,
 * the requesting student, the desired course details, and
 * optional notes.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KursErstellungsAnfrageDTO {

    /**
     * Unique identifier of the creation request.
     */
    private UUID anfrageID;

    /**
     * Identifier of the requesting student.
     */
    private UUID studentID;

    /**
     * Full name of the requesting student.
     */
    private String studentName;

    /**
     * Desired name of the course to be created.
     */
    private String kursName;

    /**
     * Desired cohort year of the course to be created.
     */
    private String jahrgang;

    /**
     * Date on which the request was submitted.
     */
    private LocalDate anfrageDatum;

    /**
     * Optional notes provided with the request.
     */
    private String requestNotes;
}
