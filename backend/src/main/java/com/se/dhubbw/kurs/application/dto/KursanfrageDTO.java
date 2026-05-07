package com.se.dhubbw.kurs.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO representing a pending course enrollment request.
 *
 * <p>Carries the data a course administrator needs to review
 * an open join request: the request identifier, the target
 * course, the requesting student, and the submission date.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KursanfrageDTO {

    /**
     * Unique identifier of the enrollment request.
     */
    private UUID anfrageID;

    /**
     * Identifier of the target course.
     */
    private UUID kursID;

    /**
     * Name of the target course.
     */
    private String kursName;

    /**
     * Identifier of the requesting student.
     */
    private UUID studentID;

    /**
     * Full name of the requesting student.
     */
    private String studentName;

    /**
     * Date on which the request was submitted.
     */
    private LocalDate anfrageDatum;
}
