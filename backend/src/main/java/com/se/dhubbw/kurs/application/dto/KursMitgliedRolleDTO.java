package com.se.dhubbw.kurs.application.dto;

import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a course member with their
 * role.
 *
 * <p>Used by the system-admin endpoint
 * {@code retrieveKursMembersAndTheirRoles} to expose the name
 * and role of each approved member of a given course.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KursMitgliedRolleDTO {

    /**
     * Full name of the course member.
     */
    private String studentName;

    /**
     * Current role of the course member.
     */
    private StudentType rolle;
}
