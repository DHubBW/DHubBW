package com.se.dhubbw.student.application.dto.output;

import com.se.dhubbw.student.infrastructure.util.StudentType;

import java.util.UUID;

/**
 * Data transfer object for the student profile.
 * Contains name, email, role, course name, and pending course request.
 *
 * <p>Contains the student's name, email address, role,
 * current course name (if enrolled), and any open course
 * request (if present).</p>
 *
 * @param name                full name of the student
 * @param dhbwMail            DHBW email address of the student
 * @param studentType         current role of the student
 * @param kursID              ID of the course the student
 * is enrolled in (null if none)
 * @param kursName            name of the course the student
 * is enrolled in (null if none)
 * @param pendingKursRequest  open course request of the student (null if none)
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
public record StudentProfileDTO(
    String name, String dhbwMail, StudentType studentType,
    UUID kursID, String kursName, PendingKursRequestDTO pendingKursRequest) { }
