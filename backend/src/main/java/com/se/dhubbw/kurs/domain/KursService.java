package com.se.dhubbw.kurs.domain;

import com.se.dhubbw.kurs.application.dto.KursYearDTO;

import java.util.List;
import java.util.UUID;

/**
 * Domain interface for course-related use cases.
 * Defines operations for listing courses, submitting join requests, and
 * creation requests.
 *
 * <p>Defines operations for retrieving courses, opening join
 * requests, and opening course creation requests.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface, technology-free)
 */
public interface KursService {

    /**
     * Retrieve all available courses (name and cohort year).
     *
     * @return list of course names and cohort years as DTOs
     */
    List<KursYearDTO> retrieveKursNamesAndYears();

    /**
     * Open a join request for a specific course.
     *
     * <p>The student must not already be enrolled in any course,
     * and must not have another pending join request.</p>
     *
     * @param dhbwMail the DHBW email of the requesting student
     * @param kursId the ID of the course to join
     * @throws org.springframework.web.server.ResponseStatusException
     *     if validation fails
     */
    void openJoinKursRequest(UUID kursId, String dhbwMail);

    /**
     * Open a course creation request.
     *
     * <p>The requesting student must not already be enrolled in
     * any course. A course with the same name and cohort year
     * must not already exist.</p>
     *
     * @param kursName the name of the course to create
     * @param jahrgang the cohort year of the course
     * @param additionalNotes optional notes for the course creation
     * @param dhbwMail the DHBW email of the requesting student
     * @throws org.springframework.web.server.ResponseStatusException
     *     if validation fails
     */
    void openCreateCourseRequest(String kursName, String jahrgang,
            String additionalNotes, String dhbwMail);
}
