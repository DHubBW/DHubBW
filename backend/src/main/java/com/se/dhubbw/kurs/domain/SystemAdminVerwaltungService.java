package com.se.dhubbw.kurs.domain;

import com.se.dhubbw.kurs.application.dto.KursMitgliedRolleDTO;
import com.se.dhubbw.kurs.application.dto.KursDTO;
import com.se.dhubbw.kurs.application.dto.StudentDTO;
import com.se.dhubbw.kurs.application.dto.SystemAdminStatsDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for system-admin course and student management.
 *
 * <p>Provides operations for a SYSTEMADMIN to retrieve platform
 * statistics, list and delete courses (with full cascade),
 * list course members with their roles, list all students, and
 * manage the SYSTEMADMIN role.</p>
 */
public interface SystemAdminVerwaltungService {

    /**
     * Retrieve platform-wide statistics.
     *
     * @return a {@link SystemAdminStatsDTO} with total counts for
     *     courses, users, files, used storage, available storage,
     *     and pending course creation requests
     */
    SystemAdminStatsDTO retrieveSystemAdminStats();

    /**
     * Retrieve all courses on the platform.
     *
     * @return list of all courses as {@link KursDTO}; may be empty
     */
    List<KursDTO> retrieveKurse();

    /**
     * Delete a course and handle its associated folders and files.
     *
     * @param kursId the ID of the course to delete
     * @throws java.util.NoSuchElementException if no course with
     *     the given ID exists
     */
    void deleteKurs(UUID kursId);

    /**
     * Retrieve all approved members of a course with their roles.
     *
     * @param kursId the ID of the course to query
     * @return list of {@link KursMitgliedRolleDTO}; may be empty
     * @throws java.util.NoSuchElementException if no course with
     *     the given ID exists
     */
    List<KursMitgliedRolleDTO> retrieveKursMembersAndTheirRoles(
        UUID kursId);

    /**
     * Retrieve all registered students on the platform.
     *
     * @return list of all students as {@link StudentDTO}; may be
     *     empty
     */
    List<StudentDTO> retrieveAllStudents();

    /**
     * Promote a student to the SYSTEMADMIN role.
     *
     * @param studentId the ID of the student to promote
     * @throws java.util.NoSuchElementException if no student with
     *     the given ID exists
     * @throws IllegalStateException if the student is already a
     *     SYSTEMADMIN
     */
    void addSysAdmin(UUID studentId);

    /**
     * Revoke the SYSTEMADMIN role from a student.
     *
     * <p>At least one other SYSTEMADMIN must remain on the
     * platform after the operation.</p>
     *
     * @param studentId the ID of the SYSTEMADMIN to demote
     * @throws java.util.NoSuchElementException if no student with
     *     the given ID exists
     * @throws IllegalArgumentException if the student is not a
     *     SYSTEMADMIN
     * @throws IllegalStateException if the student is the only
     *     SYSTEMADMIN on the platform
     */
    void removeSysAdmin(UUID studentId);
}
