package com.se.dhubbw.kurs.application.dto;

import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing a student.
 *
 * <p>Used by the system-admin endpoint to expose all registered
 * students with their identifier, display name, DHBW email
 * address, and current role.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StudentDTO {

    /**
     * Unique identifier of the student.
     */
    private UUID studentID;

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
}
