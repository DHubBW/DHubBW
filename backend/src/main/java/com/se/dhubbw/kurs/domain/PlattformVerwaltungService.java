package com.se.dhubbw.kurs.domain;

import com.se.dhubbw.kurs.application.dto.output.KursMitgliedRolleDTO;
import com.se.dhubbw.kurs.application.dto.output.KursDTO;
import com.se.dhubbw.kurs.application.dto.output.StudentDTO;
import com.se.dhubbw.kurs.application.dto.output.SystemAdminStatsDTO;

import java.util.List;
import java.util.UUID;

/**
 * Domain interface for platform-wide management by system admins.
 * Defines operations for statistics, course management, student lists,
 * and SYSTEMADMIN roles.
 *
 * <p>Provides operations for a SYSTEMADMIN to retrieve platform
 * statistics, list and delete courses (with full cascade),
 * list course members with their roles, list all students, and
 * manage the SYSTEMADMIN role.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface, technology-free)
 */
public interface PlattformVerwaltungService {

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

    /**
     * Promote a course member to KURSADMIN (system-admin variant).
     *
     * <p>The student must be an approved member of the given course
     * and must not already be KURSADMIN or SYSTEMADMIN.</p>
     *
     * @param kursId    the ID of the course
     * @param studentId the ID of the student to promote
     * @throws java.util.NoSuchElementException if the course or
     *     student is not found, or the student is not a member
     * @throws IllegalStateException if already KURSADMIN
     * @throws IllegalArgumentException if target is SYSTEMADMIN
     */
    void addKursadminForCourse(UUID kursId, UUID studentId);

    /**
     * Revoke the KURSADMIN role from a course member
     * (system-admin variant).
     *
     * <p>The student must be a KURSADMIN and approved member of
     * the given course.</p>
     *
     * @param kursId    the ID of the course
     * @param studentId the ID of the student to demote
     * @throws java.util.NoSuchElementException if the course or
     *     student is not found, or the student is not a member
     * @throws IllegalArgumentException if target is not KURSADMIN
     * @throws IllegalStateException if target is the only KURSADMIN
     */
    void removeKursadminForCourse(UUID kursId, UUID studentId);

    /**
     * Delete a student account on behalf of a SYSTEMADMIN, with
     * email confirmation.
     *
     * <p>The cleanup performed is identical to a self-service
     * account deletion: enrollment requests, creation tracking
     * records, file uploader references and the student record
     * itself. Files and folders are preserved.</p>
     *
     * <p>Validation order:</p>
     * <ol>
     *   <li>Target student must exist (404 otherwise).</li>
     *   <li>Caller must exist (404 otherwise).</li>
     *   <li>Caller must not delete their own account through
     *       this endpoint — sysadmins must use self-service
     *       (400 otherwise).</li>
     *   <li>{@code confirmedMail} must match the target's
     *       {@code dhbwMail} after trim + lower-case compare
     *       (400 otherwise).</li>
     *   <li>The same last-SYSTEMADMIN and last-KURSADMIN guards
     *       enforced by the self-service deletion apply
     *       (409 otherwise).</li>
     * </ol>
     *
     * @param studentId     the ID of the student to delete
     * @param confirmedMail the DHBW email of the target as typed
     *     into the confirmation field by the SYSTEMADMIN
     * @param callerMail    the authenticated SYSTEMADMIN's email,
     *     resolved from {@code Authentication#getName()}
     * @throws java.util.NoSuchElementException if the target
     *     student or the caller is not found
     * @throws IllegalArgumentException if the caller targets
     *     their own account, or {@code confirmedMail} does not
     *     match the target's email
     * @throws IllegalStateException if removing the target would
     *     leave zero SYSTEMADMINs, or strand a course with zero
     *     KURSADMINs
     */
    void deleteStudent(UUID studentId, String confirmedMail,
        String callerMail);
}
