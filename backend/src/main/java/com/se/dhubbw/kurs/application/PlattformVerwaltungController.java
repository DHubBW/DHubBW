package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.application.dto.KursDTO;
import com.se.dhubbw.kurs.application.dto.KursMitgliedRolleDTO;
import com.se.dhubbw.kurs.application.dto.StudentDTO;
import com.se.dhubbw.kurs.application.dto.SystemAdminStatsDTO;
import com.se.dhubbw.kurs.domain.PlattformVerwaltungService;
import com.se.dhubbw.shared.application.dto.RequestStatusResponseDTO;
import com.se.dhubbw.shared.application.dto.util.PersistenceOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * REST controller for system-admin course and student management.
 *
 * <p>Exposes endpoints for a SYSTEMADMIN to retrieve platform
 * statistics, list and delete courses, inspect course members,
 * list all students, and manage the SYSTEMADMIN role.
 * All endpoints require the {@code SYSTEMADMIN} role.</p>
 */
@RestController
@RequestMapping("/systemadmin")
@RequiredArgsConstructor
public class PlattformVerwaltungController {

    /**
     * Service handling platform management business logic.
     */
    private final PlattformVerwaltungService
        plattformVerwaltungService;

    /**
     * Retrieve platform-wide statistics.
     *
     * @return 200 with a {@link SystemAdminStatsDTO} containing
     *     total counts for courses, users, files, storage
     *     usage, and pending course creation requests
     */
    @Operation(
            summary = "Retrieve platform statistics",
            description = "Returns total counts for courses, "
                + "users, files, used/available storage, and "
                + "pending course creation requests."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Statistics successfully retrieved",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation =
                                SystemAdminStatsDTO.class)
            )
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<SystemAdminStatsDTO>
            retrieveSystemAdminStats() {
        return ResponseEntity.ok(
            plattformVerwaltungService.retrieveSystemAdminStats());
    }

    /**
     * Retrieve all courses on the platform.
     *
     * @return 200 with the list of all courses
     */
    @Operation(
            summary = "Retrieve all courses",
            description = "Returns all courses currently on "
                + "the platform."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Courses successfully retrieved",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation = KursDTO.class))
            )
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @GetMapping("/kurse")
    public ResponseEntity<List<KursDTO>> retrieveKurse() {
        return ResponseEntity.ok(
            plattformVerwaltungService.retrieveKurse());
    }

    /**
     * Delete a course and all its associated data.
     *
     * <p>Performs a full cascade in the following order:</p>
     * <ol>
     *   <li><b>Private folders</b> of the course — deleted along
     *       with all their files, file creation records, and the
     *       folder creation record.</li>
     *   <li><b>Public folders</b> — detached from the course
     *       (course reference cleared) and kept available
     *       platform-wide; their files are not removed.</li>
     *   <li><b>Enrollments</b>
     *       ({@code KursBeitrittsAnfrage}) — all approved and
     *       pending enrollments are deleted. Members holding the
     *       {@code KURSADMIN} role are demoted to {@code STUDENT}
     *       before removal.</li>
     *   <li><b>Course creation requests</b>
     *       ({@code KursErstellungsAnfrage}) — every request
     *       (approved or pending) whose desired course name and
     *       cohort year match the deleted course is removed, so
     *       no historical or pending request remains pointing to a
     *       course that no longer exists.</li>
     *   <li><b>The course entity</b> itself.</li>
     * </ol>
     *
     * <p>User accounts are never removed by this cascade. The
     * deletion runs in a single transaction and is retried on
     * transient datastore errors.</p>
     *
     * @param kursId the ID of the course to delete
     * @return 204 on success, 404 if the course is not found
     */
    @Operation(
            summary = "Delete a course",
            description = "Deletes a course with full cascade. "
                + "Private folders (with files and creation "
                + "records) are removed; public folders are "
                + "detached from the course and kept. All "
                + "enrollments are deleted, KURSADMIN members "
                + "demoted to STUDENT. All course creation "
                + "requests targeting the same course "
                + "name/cohort year are removed. User accounts "
                + "are not affected."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Course deleted successfully"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Course not found"
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @DeleteMapping("/kurse/{kursId}")
    public ResponseEntity<RequestStatusResponseDTO> deleteKurs(
            @PathVariable final UUID kursId) {
        plattformVerwaltungService.deleteKurs(kursId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(new RequestStatusResponseDTO(
            "Kurs deleted successfully.", PersistenceOperation.DELETE));

    }

    /**
     * Retrieve all approved members of a course with their roles.
     *
     * @param kursId the ID of the course to query
     * @return 200 with the member-role list, or 404 if the course
     *     is not found
     */
    @Operation(
            summary = "Retrieve course members and their roles",
            description = "Returns the name and role of each "
                + "approved member of the given course."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Members successfully retrieved",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation =
                                        KursMitgliedRolleDTO.class))
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Course not found"
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @GetMapping("/kurse/{kursId}/mitglieder")
    public ResponseEntity<List<KursMitgliedRolleDTO>>
            retrieveKursMembersAndTheirRoles(
            @PathVariable final UUID kursId) {
        try {
            return ResponseEntity.ok(
                plattformVerwaltungService
                    .retrieveKursMembersAndTheirRoles(kursId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Retrieve all registered students on the platform.
     *
     * @return 200 with the list of all students
     */
    @Operation(
            summary = "Retrieve all students",
            description = "Returns all students registered "
                + "on the platform."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Students successfully retrieved",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation =
                                        StudentDTO.class))
            )
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @GetMapping("/studenten")
    public ResponseEntity<List<StudentDTO>> retrieveAllStudents() {
        return ResponseEntity.ok(
            plattformVerwaltungService.retrieveAllStudents());
    }

    /**
     * Promote a student to the SYSTEMADMIN role.
     *
     * @param studentId the ID of the student to promote
     * @return 200 on success, 404 if the student is not found,
     *     409 if the student is already a SYSTEMADMIN
     */
    @Operation(
            summary = "Promote student to SYSTEMADMIN",
            description = "Grants the SYSTEMADMIN role to a "
                + "student."
    )
    @ApiResponse(
            responseCode = "200",
            description = "SYSTEMADMIN role granted"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Student not found"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Student is already SYSTEMADMIN"
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @PatchMapping("/studenten/{studentId}/add-sysadmin")
    public ResponseEntity<RequestStatusResponseDTO> addSysAdmin(
            @PathVariable final UUID studentId) {
        plattformVerwaltungService.addSysAdmin(studentId);
        return ResponseEntity.status(HttpStatus.OK).body(
            new RequestStatusResponseDTO("SysAdmin added successfully",
                PersistenceOperation.UPDATE));
    }

    /**
     * Revoke the SYSTEMADMIN role from a student.
     *
     * <p>At least one other SYSTEMADMIN must remain after the
     * operation. The demoted student's role is set to
     * {@code STUDENT}.</p>
     *
     * @param studentId the ID of the SYSTEMADMIN to demote
     * @return 200 on success, 400 if the student is not a
     *     SYSTEMADMIN, 404 if the student is not found,
     *     409 if the student is the only SYSTEMADMIN
     */
    @Operation(
            summary = "Revoke SYSTEMADMIN role",
            description = "Removes the SYSTEMADMIN role from a "
                + "student. At least one other SYSTEMADMIN must "
                + "remain."
    )
    @ApiResponse(
            responseCode = "200",
            description = "SYSTEMADMIN role revoked"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Student is not a SYSTEMADMIN"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Student not found"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Student is the only SYSTEMADMIN"
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @PatchMapping("/studenten/{studentId}/remove-sysadmin")
    public ResponseEntity<RequestStatusResponseDTO> removeSysAdmin(
            @PathVariable final UUID studentId) {
        plattformVerwaltungService.removeSysAdmin(studentId);
        return ResponseEntity.status(HttpStatus.OK).body(
            new RequestStatusResponseDTO(
            "SysAdmin removed successfully.", PersistenceOperation.UPDATE));
    }
}
