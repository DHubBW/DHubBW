package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.application.dto.output.KursAdminStatsDTO;
import com.se.dhubbw.kurs.application.dto.output.KursmemberDTO;
import com.se.dhubbw.kurs.infrastructure.KursMitgliederServiceImpl;
import com.se.dhubbw.shared.application.dto.output.RequestStatusResponseDTO;
import com.se.dhubbw.shared.application.dto.util.PersistenceOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for course member and role management (statistics,
 * members, roles).
 * Provides HTTP endpoints for KURSADMIN to manage course members.
 *
 * <p>Exposes endpoints for a KURSADMIN to retrieve course
 * statistics, list members, manage KURSADMIN roles, and
 * remove members from their course. All endpoints require
 * the {@code KURSADMIN} role.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category T-Software (REST endpoints with Spring Web
 *     annotations)
 * @see KursMitgliederServiceImpl
 */
@RestController
@RequestMapping("/kursadmin/mitglieder")
@RequiredArgsConstructor
public class KursMitgliederController {

    /**
     * Service handling member and role management logic.
     */
    private final KursMitgliederServiceImpl kursMitgliederService;

    /**
     * Retrieve aggregated statistics for the admin's course.
     *
     * @param authentication the current user's security context
     * @return 200 with stats, or 404 if the admin has no course
     */
    @Operation(
            summary = "Retrieve course admin statistics",
            description = "Returns member count, file count, "
                + "and open request count for the admin's course."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Statistics successfully retrieved",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = KursAdminStatsDTO.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Admin has no course assignment",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('KURSADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<KursAdminStatsDTO> retrieveKursAdminStats(
            final Authentication authentication) {
        KursAdminStatsDTO stats =
            kursMitgliederService.retrieveKursAdminStats(
                authentication.getName());
        return ResponseEntity.ok(stats);
    }

    /**
     * Retrieve all approved members of the admin's course.
     *
     * @param authentication the current user's security context
     * @return 200 with member list, or 404 if no course found
     */
    @Operation(
            summary = "Retrieve all course members",
            description = "Returns all approved members of "
                + "the admin's course."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Members successfully retrieved",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation
                                        = KursmemberDTO.class))
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Admin has no course assignment",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('KURSADMIN')")
    @GetMapping
    public ResponseEntity<List<KursmemberDTO>>
            retrieveAllCourseMembers(
            final Authentication authentication) {
        List<KursmemberDTO> members =
            kursMitgliederService.retrieveAllCourseMembers(
                authentication.getName());
        return ResponseEntity.ok(members);
    }

    /**
     * Give up the calling admin's KURSADMIN role.
     *
     * <p>The admin's student type is set to {@code STUDENT}.
     * Requires at least one other KURSADMIN in the course.</p>
     *
     * @param authentication the current user's security context
     * @return 204 on success, 404 if admin has no course,
     *     or 409 if the admin is the only KURSADMIN
     */
    @Operation(
            summary = "Give up KURSADMIN role",
            description = "Gives up the caller's KURSADMIN "
                + "role. Requires min. 1 other KURSADMIN."
    )
    @ApiResponse(responseCode = "204", description = "Role given up")
    @ApiResponse(
            responseCode = "404",
            description = "Admin has no course assignment"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Caller is the only KURSADMIN"
    )
    @PreAuthorize("hasRole('KURSADMIN')")
    @PatchMapping("/resign")
    public ResponseEntity<RequestStatusResponseDTO> kursadminNiederlegen(
            final Authentication authentication) {
        kursMitgliederService.kursadminNiederlegen(
            authentication.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(new RequestStatusResponseDTO(
                "You gave up being kursadmin successfully.",
                PersistenceOperation.UPDATE));
    }

    /**
     * Promote a course member to KURSADMIN.
     *
     * <p>Target must be a {@code STUDENT} in the admin's course.
     * SYSTEMADMIN cannot be promoted.</p>
     *
     * @param studentId      the ID of the student to promote
     * @param authentication the current user's security context
     * @return 204 on success, 400 if the target is SYSTEMADMIN,
     *     404 if not found or not in course,
     *     or 409 if already KURSADMIN
     */
    @Operation(
            summary = "Promote student to KURSADMIN",
            description = "Grants KURSADMIN role to a course "
                + "member. Target must not be SYSTEMADMIN."
    )
    @ApiResponse(responseCode = "204", description = "Role granted")
    @ApiResponse(
            responseCode = "400",
            description = "Target is SYSTEMADMIN"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Student not found or not in course"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Student is already KURSADMIN"
    )
    @PreAuthorize("hasRole('KURSADMIN')")
    @PatchMapping("/add-kursadmin/{studentId}")
    public ResponseEntity<RequestStatusResponseDTO> addKursadmin(
            @PathVariable final UUID studentId,
            final Authentication authentication) {
        kursMitgliederService.addKursadmin(
            studentId, authentication.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
            new RequestStatusResponseDTO(
            "Kursadmin added successully.", PersistenceOperation.UPDATE));
    }

    /**
     * Revoke a course member's KURSADMIN role.
     *
     * <p>Target must be a {@code KURSADMIN} in the admin's
     * course. At least one other KURSADMIN must remain.</p>
     *
     * @param studentId      the ID of the KURSADMIN to demote
     * @param authentication the current user's security context
     * @return 204 on success, 400 if target is not KURSADMIN,
     *     404 if not found or not in course,
     *     or 409 if target is the only KURSADMIN
     */
    @Operation(
            summary = "Revoke KURSADMIN role",
            description = "Removes KURSADMIN role from a "
                + "course member. Min. 1 Kursadmin must remain."
    )
    @ApiResponse(responseCode = "200", description = "Role revoked")
    @ApiResponse(
            responseCode = "400",
            description = "Target is not a KURSADMIN"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Student not found or not in course"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Target is the only KURSADMIN"
    )
    @PreAuthorize("hasRole('KURSADMIN')")
    @PatchMapping("/remove-kursadmin/{studentId}")
    public ResponseEntity<RequestStatusResponseDTO> removeKursadmin(
            @PathVariable final UUID studentId,
            final Authentication authentication) {
        kursMitgliederService.removeKursadmin(
            studentId, authentication.getName());
        return ResponseEntity.status(
            HttpStatus.OK).body(new RequestStatusResponseDTO(
                "Kursadmin removed successfully.",
            PersistenceOperation.UPDATE));
    }

    /**
     * Remove a student from the course.
     *
     * <p>Target must be a {@code STUDENT} (not KURSADMIN or
     * SYSTEMADMIN) and an approved member of the admin's
     * course.</p>
     *
     * @param studentId      the ID of the student to remove
     * @param authentication the current user's security context
     * @return 204 on success, 404 if not found or not in course,
     *     or 409 if the target has an elevated role
     */
    @Operation(
            summary = "Remove course member",
            description = "Removes a STUDENT from the course. "
                + "KURSADMIN members cannot be removed."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Member removed from course"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Student not found or not in course"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Target is KURSADMIN or SYSTEMADMIN"
    )
    @PreAuthorize("hasRole('KURSADMIN')")
    @DeleteMapping("/{studentId}")
    public ResponseEntity<RequestStatusResponseDTO> removeKursMember(
            @PathVariable final UUID studentId,
            final Authentication authentication) {
        kursMitgliederService.removeKursMember(
            studentId, authentication.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
            new RequestStatusResponseDTO(
                "You removed the kurs member successfully.",
                PersistenceOperation.UPDATE));
    }
}
