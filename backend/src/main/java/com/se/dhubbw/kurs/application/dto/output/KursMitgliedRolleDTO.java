package com.se.dhubbw.kurs.application.dto.output;

import com.se.dhubbw.student.infrastructure.util.StudentType;

import java.util.UUID;

/**
 * Data transfer object for a course member with role (system admin view).
 * Contains id, name and role of an approved course member.
 *
 * <p>Used by the system-admin endpoint
 * {@code retrieveKursMembersAndTheirRoles} to expose the id, name
 * and role of each approved member of a given course.</p>
 *
 * @param studentID   unique identifier of the course member
 * @param studentName full name of the course member
 * @param rolle       current role of the course member
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
public record KursMitgliedRolleDTO(
    UUID studentID,
    String studentName,
    StudentType rolle
) { }
