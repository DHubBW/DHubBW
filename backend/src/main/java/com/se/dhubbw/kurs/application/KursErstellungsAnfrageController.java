package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.application.dto.KursErstellungsAnfrageDTO;
import com.se.dhubbw.kurs.domain.KursSystemAdminService;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.UUID;

/**
 * REST controller for system-admin course creation request
 * management.
 *
 * <p>Exposes endpoints for a SYSTEMADMIN to list, approve, or
 * decline pending course creation requests. All endpoints
 * require the {@code SYSTEMADMIN} role.</p>
 */
@RestController
@RequestMapping("/systemadmin/anfragen")
@RequiredArgsConstructor
public class KursErstellungsAnfrageController {

    /**
     * Service handling the course creation request business logic.
     */
    private final KursSystemAdminService kursSystemAdminService;

    /**
     * Retrieve all pending course creation requests.
     *
     * @return 200 with the list of pending requests
     */
    @Operation(
            summary = "Retrieve pending course creation requests",
            description = "Returns all open course creation requests "
                + "awaiting system-admin review."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Pending requests successfully loaded",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation =
                                        KursErstellungsAnfrageDTO.class))
            )
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @GetMapping
    public ResponseEntity<List<KursErstellungsAnfrageDTO>>
            retrieveKursErstellungsanfragen() {
        List<KursErstellungsAnfrage> anfragen =
            kursSystemAdminService.retrieveKursErstellungsanfragen();
        List<KursErstellungsAnfrageDTO> dtos = anfragen.stream()
            .map(KursErstellungsAnfrageController::mapErstellungsAnfrageToDto)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Approve a pending course creation request.
     *
     * <p>Creates the course, promotes the requester to
     * {@code KURSADMIN}, and logs course membership.</p>
     *
     * @param id the ID of the request to approve
     * @return 200 on success, 404 if the request is not found,
     *     409 if the student is already enrolled or the course
     *     already exists
     */
    @Operation(
            summary = "Approve course creation request",
            description = "Creates the course, promotes the "
                + "requesting student to KURSADMIN, and logs "
                + "course membership."
    )
    @ApiResponse(responseCode = "200", description = "Request approved")
    @ApiResponse(
            responseCode = "404",
            description = "Request not found"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Student already enrolled or course exists"
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approveKursErstellungsanfrage(
            @PathVariable final UUID id) {
        try {
            kursSystemAdminService.approveKursErstellungsanfrage(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Decline and permanently delete a pending course creation
     * request.
     *
     * @param id the ID of the request to decline
     * @return 204 on success, 404 if the request is not found
     */
    @Operation(
            summary = "Decline course creation request",
            description = "Declines and removes a course creation "
                + "request."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Request declined and deleted"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Request not found"
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> declineKursErstellungsanfrage(
            @PathVariable final UUID id) {
        kursSystemAdminService.declineKursErstellungsanfrage(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Map a {@link KursErstellungsAnfrage} entity to its DTO.
     *
     * @param anfrage the entity to convert
     * @return the corresponding {@link KursErstellungsAnfrageDTO}
     */
    private static KursErstellungsAnfrageDTO mapErstellungsAnfrageToDto(
            final KursErstellungsAnfrage anfrage) {
        return new KursErstellungsAnfrageDTO(
            anfrage.getAnfrageID(),
            anfrage.getStudent().getStudentID(),
            anfrage.getStudent().getName(),
            anfrage.getKursName(),
            anfrage.getJahrgang(),
            anfrage.getAnfrageDatum(),
            anfrage.getRequestNotes()
        );
    }
}
