package com.se.dhubbw.kurs.application.dto.input;

/**
 * Request body for the SYSTEMADMIN-initiated student account
 * deletion endpoint.
 *
 * <p>Carries the email address that the SYSTEMADMIN typed into
 * the confirmation field of the delete dialog. The service
 * compares this value against the target student's
 * {@code dhbwMail} (trim + lower-case) and refuses the deletion
 * on mismatch.</p>
 *
 * @param confirmedMail the email address typed for confirmation;
 *     must equal the target's DHBW mail (case-insensitive,
 *     trimmed) for the request to proceed
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     HTTP Request → Service)
 */
public record DeleteStudentRequestDTO(String confirmedMail) {
}
