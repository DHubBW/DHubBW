package com.se.dhubbw.kurs.application.dto.output;

import com.se.dhubbw.student.infrastructure.util.StudentType;

import java.util.UUID;

/**
 * Data transfer object for representing a student (system admin view).
 * Contains ID, name, email, and role for the platform-wide student list.
 *
 * <p>Used by the system-admin endpoint to expose all registered
 * students with their identifier, display name, DHBW email
 * address, and current role.</p>
 *
 * @param studentID   unique identifier of the student
 * @param name        full name of the student
 * @param dhbwMail    DHBW email address of the student
 * @param studentType current role of the student
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
public record StudentDTO(
    UUID studentID,
    String name,
    String dhbwMail,
    StudentType studentType
) { }
