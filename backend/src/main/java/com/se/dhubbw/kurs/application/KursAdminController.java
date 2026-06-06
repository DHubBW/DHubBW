package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.application.dto.KursanfrageDTO;
import com.se.dhubbw.kurs.domain.KursAdminService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for course admin enrollment request management
 * (list, approve, decline).
 * Provides HTTP endpoints for KURSADMIN to manage open enrollment requests.
 *
 * <p>Exposes endpoints for a KURSADMIN to list, approve, or
 * decline pending join requests for their course. All endpoints
 * require the {@code KURSADMIN} role.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category T-Software (REST endpoints with Spring Web
 *     annotations)
 * @see KursAdminService
 */
@RestController
@RequestMapping("/kursadmin/anfragen")
@RequiredArgsConstructor
public class KursAdminController {

    /**
     * Service handling the enrollment request business logic.
     */
    private final KursAdminService kursAdminService;

    /**
     * Retrieve all open enrollment requests for the admin's course.
     *
     * @param authentication the current user's security context
     * @return 200 with the list of pending requests, or 404 if the
     *     admin has no approved course assignment
     */
    @Operation(
            summary = "Retrieve pending enrollment requests",
            description = "Returns all open join requests "
                + "for the admin's course."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Pending requests successfully loaded",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation = KursanfrageDTO.class))
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Admin has no course assignment",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('KURSADMIN')")
    @GetMapping
    public ResponseEntity<List<KursanfrageDTO>>
            retrieveKursbeitrittsanfragen(
            final Authentication authentication) {
        return ResponseEntity.ok(
            kursAdminService.retrieveKursbeitrittsanfragen(
                authentication.getName()));
    }

    /**
     * Approve a pending enrollment request, assigning the student
     * to the course.
     *
     * @param id the ID of the request to approve
     * @param authentication the current user's security context
     * @return 200 on success, 404 if the request is not found,
     *     or 403 if the request belongs to another course
     */
    @Operation(
            summary = "Approve enrollment request",
            description = "Approves a join request and "
                + "assigns the student to the course."
    )
    @ApiResponse(responseCode = "200", description = "Request approved")
    @ApiResponse(
            responseCode = "404",
            description = "Request not found"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Request belongs to another course"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Student is already enrolled in a course"
    )
    @PreAuthorize("hasRole('KURSADMIN')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<RequestStatusResponseDTO> approveKursbeitrittsanfrage(
            @PathVariable final UUID id,
            final Authentication authentication) {
        try {
            kursAdminService.approveKursbeitrittsanfrage(
                id, authentication.getName());
            return ResponseEntity.status(HttpStatus.OK).body(
                new RequestStatusResponseDTO(
                    "Kursbeitrittsanfrage approved successfully.",
                    PersistenceOperation.UPDATE));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Decline and permanently delete a pending enrollment request.
     *
     * @param id the ID of the request to decline
     * @param authentication the current user's security context
     * @return 204 on success, 404 if the request is not found,
     *     or 403 if the request belongs to another course
     */
    @Operation(
            summary = "Decline enrollment request",
            description = "Declines and removes a join request."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Request declined and deleted"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Request not found"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Request belongs to another course"
    )
    @PreAuthorize("hasRole('KURSADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<RequestStatusResponseDTO> declineKursbeitrittsanfrage(
            @PathVariable final UUID id,
            final Authentication authentication) {
        try {
            kursAdminService.declineKursbeitrittsanfrage(
                id, authentication.getName());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
                new RequestStatusResponseDTO(
                "Request deleted successfully", PersistenceOperation.DELETE));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
