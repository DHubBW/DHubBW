package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.application.dto.KursDTO;
import com.se.dhubbw.kurs.application.dto.KursMitgliedRolleDTO;
import com.se.dhubbw.kurs.application.dto.StudentDTO;
import com.se.dhubbw.kurs.application.dto.SystemAdminStatsDTO;
import com.se.dhubbw.kurs.domain.SystemAdminVerwaltungService;
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
public class SystemAdminVerwaltungController {

    /**
     * Service handling platform management business logic.
     */
    private final SystemAdminVerwaltungService
        systemAdminVerwaltungService;

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
            systemAdminVerwaltungService.retrieveSystemAdminStats());
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
            systemAdminVerwaltungService.retrieveKurse());
    }

    /**
     * Delete a course and all its associated data.
     *
     * <p>Cascade includes folders, files, creation records,
     * and enrollments. KURSADMIN members are demoted to
     * STUDENT.</p>
     *
     * @param kursId the ID of the course to delete
     * @return 204 on success, 404 if the course is not found
     */
    @Operation(
            summary = "Delete a course",
            description = "Deletes a course with full cascade: "
                + "folders, files, creation records, and "
                + "enrollments. KURSADMIN members are demoted "
                + "to STUDENT."
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
    public ResponseEntity<Void> deleteKurs(
            @PathVariable final UUID kursId) {
        try {
            systemAdminVerwaltungService.deleteKurs(kursId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
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
                systemAdminVerwaltungService
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
            systemAdminVerwaltungService.retrieveAllStudents());
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
    public ResponseEntity<Void> addSysAdmin(
            @PathVariable final UUID studentId) {
        try {
            systemAdminVerwaltungService.addSysAdmin(studentId);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
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
    public ResponseEntity<Void> removeSysAdmin(
            @PathVariable final UUID studentId) {
        try {
            systemAdminVerwaltungService.removeSysAdmin(studentId);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
