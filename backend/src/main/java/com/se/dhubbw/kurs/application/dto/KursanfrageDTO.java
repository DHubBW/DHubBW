package com.se.dhubbw.kurs.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data transfer object for a pending course enrollment request.
 * Contains the data a course admin needs to review an enrollment request.
 *
 * <p>Carries the data a course administrator needs to review
 * an open join request: the request identifier, the target
 * course, the requesting student, and the submission date.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 * @param anfrageID Unique identifier of the enrollment request.
 * @param kursID Identifier of the target course.
 * @param kursName Name of the target course.
 * @param studentID Identifier of the requesting student.
 * @param studentName name of the student.
 * @param anfrageDatum The date of the request.
 */
public record KursanfrageDTO(UUID anfrageID,
                             UUID kursID,
                             String kursName,
                             UUID studentID,
                             String studentName,
                             LocalDate anfrageDatum) { }
