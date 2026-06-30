package com.se.dhubbw.kurs.application.dto.output;

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
 * @param anfrageID    unique identifier of the creation request
 * @param studentID    identifier of the requesting student
 * @param studentName  full name of the requesting student
 * @param kursName     desired name of the course to be created
 * @param jahrgang     desired cohort year of the course to be created
 * @param anfrageDatum date on which the request was submitted
 * @param requestNotes optional notes provided with the request
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
public record KursErstellungsAnfrageDTO(UUID anfrageID, UUID studentID,
                                        String studentName, String kursName,
                                        String jahrgang, LocalDate anfrageDatum,
                                        String requestNotes) { }
