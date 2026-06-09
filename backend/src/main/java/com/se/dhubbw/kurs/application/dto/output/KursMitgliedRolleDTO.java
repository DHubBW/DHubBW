package com.se.dhubbw.kurs.application.dto.output;

import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data transfer object for a course member with role (system admin view).
 * Contains id, name and role of an approved course member.
 *
 * <p>Used by the system-admin endpoint
 * {@code retrieveKursMembersAndTheirRoles} to expose the id, name
 * and role of each approved member of a given course.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KursMitgliedRolleDTO {

    /**
     * Unique identifier of the course member.
     */
    private UUID studentID;

    /**
     * Full name of the course member.
     */
    private String studentName;

    /**
     * Current role of the course member.
     */
    private StudentType rolle;
}
