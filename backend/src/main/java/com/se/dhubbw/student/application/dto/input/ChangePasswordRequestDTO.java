package com.se.dhubbw.student.application.dto.input;

/**
 * Data transfer object for password change requests.
 * Carries the current password, the new password, and a repeat
 * confirmation from client to server.
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation HTTP
 *     Request → Service)
 * @param currentPassword the student's current password (for
 *     verification).
 * @param newPassword the new password to set.
 * @param repeatPassword repeat of the new password for client-side
 *     confirmation.
 */
public record ChangePasswordRequestDTO(
    String currentPassword,
    String newPassword,
    String repeatPassword) { }
