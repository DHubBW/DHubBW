package com.se.dhubbw.student.application.dto;

import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO representing a student's profile information.
 *
 * <p>Contains the student's name, email address, role,
 * current course name (if enrolled), and any open course
 * request (if present).</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StudentProfileDTO {

    /**
     * Full name of the student.
     */
    private String name;

    /**
     * DHBW email address of the student.
     */
    private String dhbwMail;

    /**
     * Current role of the student.
     */
    private StudentType studentType;

    /**
     * Name of the course the student is enrolled in.
     * Null if the student has no course enrollment.
     */
    private String kursName;

    /**
     * Open course request of the student, if any.
     * Null if there is no pending join or creation request.
     */
    private PendingKursRequestDTO pendingKursRequest;
}
