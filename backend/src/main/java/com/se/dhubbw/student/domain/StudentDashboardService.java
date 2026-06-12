package com.se.dhubbw.student.domain;

import com.se.dhubbw.student.application.dto.output.StudentProfileDTO;

/**
 * Domain interface for student dashboard operations.
 * Defines operations for course management, account deletion, and
 * profile retrieval.
 *
 * <p>Provides operations for students to manage their own
 * course membership, account, and profile information.</p>
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface, technology-free)
 */
public interface StudentDashboardService {

    /**
     * Remove the student from their current course.
     *
     * <p>Deletes the student's approved course enrollment. If the
     * student is a KURSADMIN, this operation requires that at least
     * one other KURSADMIN remains in the course.</p>
     *
     * @param dhbwMail the DHBW mail address of the student
     * @throws java.util.NoSuchElementException if the student is not
     *     found or has no course enrollment
     * @throws IllegalStateException if the student is the last
     *     KURSADMIN in their course
     */
    void leaveKurs(String dhbwMail);

    /**
     * Delete the student's account completely.
     *
     * <p>Removes the student record and all associated metadata
     * including enrollment requests and creation tracking records.
     * The uploader reference in files is set to null, but files
     * and folders themselves are preserved. This operation fails
     * if the student is the last SYSTEMADMIN or the last KURSADMIN
     * in their course.</p>
     *
     * @param dhbwMail the DHBW mail address of the student
     * @throws java.util.NoSuchElementException if the student is not found
     * @throws IllegalStateException if the student is the last
     *     SYSTEMADMIN globally or the last KURSADMIN in their course
     */
    void deleteAccount(String dhbwMail);

    /**
     * Retrieve the profile information for the given student.
     *
     * @param dhbwMail the DHBW mail address of the student
     * @return the student's profile data including name, email,
     *     role, and course name (if enrolled)
     * @throws java.util.NoSuchElementException if the student is not found
     */
    StudentProfileDTO retrieveProfile(String dhbwMail);

    /**
     * Retrieve the course name for the given student.
     *
     * @param dhbwMail the DHBW mail address of the student
     * @return the course name, or null if the student has no
     *     course enrollment
     * @throws java.util.NoSuchElementException if the student is not found
     */
    String retrieveKursForUser(String dhbwMail);

    /**
     * Change the password of the given student.
     *
     * <p>Verifies the current password before applying the new one.
     * The new password is hashed with the configured
     * {@code PasswordEncoder} before being persisted.</p>
     *
     * @param dhbwMail the DHBW mail address of the student
     * @param currentPassword the student's current plaintext password
     * @param newPassword the new plaintext password to set
     * @throws java.util.NoSuchElementException if the student is not
     *     found
     * @throws org.springframework.web.server.ResponseStatusException
     *     401 (UNAUTHORIZED) if {@code currentPassword} does not
     *     match; 400 (BAD_REQUEST) if {@code newPassword} is empty,
     *     shorter than 8 characters, or equal to
     *     {@code currentPassword}
     */
    void changePassword(
        String dhbwMail, String currentPassword, String newPassword);
}
