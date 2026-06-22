package com.se.dhubbw.datei.application;

import com.se.dhubbw.datei.application.dto.input.CreateBewertungRequestDTO;
import com.se.dhubbw.datei.application.dto.output.DateiBewertungDTO;
import com.se.dhubbw.datei.domain.BewertungService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for file star ratings (set, remove, read).
 *
 * <p>Each user has at most one rating per file, between 1 and 5
 * whole stars. The aggregate (average + count) is held redundantly
 * on the file entity so list views never recompute it.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category T-Software (REST endpoints with Spring Web
 *     annotations)
 * @see BewertungService
 */
@RestController
@RequestMapping("/student/dateien/{dateiId}/bewertung")
@RequiredArgsConstructor
public class BewertungController {

    /**
     * Service containing all rating-management business logic.
     */
    private final BewertungService bewertungService;

    /**
     * Set or update the caller's rating on the given file.
     *
     * <p>Requires at least the {@code STUDENT} role. The caller
     * must be allowed to view the file. Stars must be a whole
     * number between 1 and 5.</p>
     *
     * @param dateiId        the UUID of the file to rate
     * @param request        the set-rating request body
     * @param authentication the current user's security context
     * @return 200 with the new aggregate plus the caller's own
     *     rating
     */
    @Operation(
            summary = "Rate a file",
            description = "Sets or updates the caller's rating on the "
                + "given file. Stars must be 1..5."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rating stored successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Stars out of range",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorised to view this file",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "File or student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping
    public ResponseEntity<DateiBewertungDTO> setBewertung(
            @Parameter(
                    description = "UUID of the file to rate",
                    required = true)
            @PathVariable final UUID dateiId,
            @RequestBody final CreateBewertungRequestDTO request,
            final Authentication authentication) {
        DateiBewertungDTO dto = bewertungService.setBewertung(
            dateiId,
            request.sterne(),
            authentication.getName());
        return ResponseEntity.ok(dto);
    }

    /**
     * Retrieve the current rating aggregate plus the caller's own
     * rating on the given file.
     *
     * @param dateiId        the UUID of the file
     * @param authentication the current user's security context
     * @return 200 with the aggregate plus the caller's own rating
     */
    @Operation(
            summary = "Get a file's rating",
            description = "Returns the current rating aggregate plus "
                + "the caller's own rating (0 if none)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rating retrieved successfully"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorised to view this file",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "File or student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping
    public ResponseEntity<DateiBewertungDTO> getBewertung(
            @Parameter(
                    description = "UUID of the file",
                    required = true)
            @PathVariable final UUID dateiId,
            final Authentication authentication) {
        DateiBewertungDTO dto = bewertungService.getBewertung(
            dateiId, authentication.getName());
        return ResponseEntity.ok(dto);
    }

    /**
     * Remove the caller's rating on the given file.
     *
     * @param dateiId        the UUID of the file
     * @param authentication the current user's security context
     * @return 200 with the updated aggregate (caller's own rating
     *     reset to 0)
     */
    @Operation(
            summary = "Remove the caller's rating",
            description = "Deletes the caller's rating on the given "
                + "file. The aggregate is updated atomically."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rating removed successfully"
    )
    @ApiResponse(
            responseCode = "404",
            description = "File, rating, or student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping
    public ResponseEntity<DateiBewertungDTO> removeBewertung(
            @Parameter(
                    description = "UUID of the file",
                    required = true)
            @PathVariable final UUID dateiId,
            final Authentication authentication) {
        DateiBewertungDTO dto = bewertungService.removeBewertung(
            dateiId, authentication.getName());
        return ResponseEntity.ok(dto);
    }
}
