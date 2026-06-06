package com.se.dhubbw.kurs.application.dto;

import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data transfer object for representing a student (system admin view).
 * Contains ID, name, email, and role for the platform-wide student list.
 *
 * <p>Used by the system-admin endpoint to expose all registered
 * students with their identifier, display name, DHBW email
 * address, and current role.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
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
