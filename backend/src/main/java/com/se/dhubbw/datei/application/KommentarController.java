package com.se.dhubbw.datei.application;

import com.se.dhubbw.datei.application.dto.input.CreateKommentarRequestDTO;
import com.se.dhubbw.datei.application.dto.input.UpdateKommentarRequestDTO;
import com.se.dhubbw.datei.application.dto.output.KommentarDTO;
import com.se.dhubbw.datei.domain.KommentarService;
import com.se.dhubbw.shared.application.dto.output.RequestStatusResponseDTO;
import com.se.dhubbw.shared.application.dto.util.PersistenceOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for file-comment operations (create, list, edit,
 * delete).
 *
 * <p>Endpoints are split between two URL prefixes that match the
 * resource hierarchy:</p>
 * <ul>
 *   <li>{@code /student/dateien/{dateiId}/kommentare} — list and
 *       create comments for a specific file.</li>
 *   <li>{@code /student/kommentare/{kommentarId}} — edit and delete
 *       a single comment by id.</li>
 * </ul>
 *
 * <p>All endpoints require at least the {@code STUDENT} role; the
 * service layer enforces fine-grained per-comment authorisation.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category T-Software (REST endpoints with Spring Web
 *     annotations)
 * @see KommentarService
 */
@RestController
@RequiredArgsConstructor
public class KommentarController {

    /**
     * Service containing all comment-management business logic.
     */
    private final KommentarService kommentarService;

    /**
     * List all comments belonging to the given file, oldest first.
     *
     * <p>Requires at least the {@code STUDENT} role. The caller
     * must be allowed to read the file.</p>
     *
     * @param dateiId        the UUID of the file
     * @param authentication the current user's security context
     * @return 200 with the list of comment DTOs
     */
    @Operation(
            summary = "List comments for a file",
            description = "Returns all comments attached to the "
                + "given file. Caller must be allowed to view "
                + "the file."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Comments retrieved successfully"
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
    @GetMapping("/student/dateien/{dateiId}/kommentare")
    public ResponseEntity<List<KommentarDTO>> retrieveKommentare(
            @Parameter(
                    description = "UUID of the file",
                    required = true)
            @PathVariable final UUID dateiId,
            final Authentication authentication) {
        List<KommentarDTO> kommentare = kommentarService
            .retrieveKommentareForDatei(
                dateiId, authentication.getName());
        return ResponseEntity.ok(kommentare);
    }

    /**
     * Create a new comment on the given file.
     *
     * <p>Requires at least the {@code STUDENT} role. The caller
     * must be allowed to read the file.</p>
     *
     * @param dateiId        the UUID of the file to comment on
     * @param request        the create-comment request body
     * @param authentication the current user's security context
     * @return 201 with the created comment DTO
     */
    @Operation(
            summary = "Create a comment on a file",
            description = "Creates a new comment on the given file. "
                + "Caller must be allowed to view the file."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Comment created successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Comment text is empty or too long",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorised to comment on this file",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "File or student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/student/dateien/{dateiId}/kommentare")
    public ResponseEntity<KommentarDTO> createKommentar(
            @Parameter(
                    description = "UUID of the file",
                    required = true)
            @PathVariable final UUID dateiId,
            @RequestBody final CreateKommentarRequestDTO request,
            final Authentication authentication) {
        KommentarDTO created = kommentarService.createKommentar(
            dateiId,
            request.inhalt(),
            authentication.getName());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(created);
    }

    /**
     * Edit an existing comment.
     *
     * <p>Requires at least the {@code STUDENT} role. Only the
     * original author may edit.</p>
     *
     * @param kommentarId    the UUID of the comment to edit
     * @param request        the edit-comment request body
     * @param authentication the current user's security context
     * @return 200 with the updated comment DTO
     */
    @Operation(
            summary = "Edit a comment",
            description = "Updates the text of an existing comment. "
                + "Only the original author may edit."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Comment updated successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Comment text is empty or too long",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "403",
            description = "Caller is not the comment author",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "Comment or student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping("/student/kommentare/{kommentarId}")
    public ResponseEntity<KommentarDTO> updateKommentar(
            @Parameter(
                    description = "UUID of the comment",
                    required = true)
            @PathVariable final UUID kommentarId,
            @RequestBody final UpdateKommentarRequestDTO request,
            final Authentication authentication) {
        KommentarDTO updated = kommentarService.updateKommentar(
            kommentarId,
            request.inhalt(),
            authentication.getName());
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a comment.
     *
     * <p>Requires at least the {@code STUDENT} role. Allowed for
     * the comment author, the file's KURSADMIN, and any
     * SYSTEMADMIN.</p>
     *
     * @param kommentarId    the UUID of the comment to delete
     * @param authentication the current user's security context
     * @return 204 if deleted successfully
     */
    @Operation(
            summary = "Delete a comment",
            description = "Permanently deletes a comment. Allowed "
                + "for the author, the file's KURSADMIN, and any "
                + "SYSTEMADMIN."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Comment deleted successfully"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorised to delete this comment",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "Comment or student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/student/kommentare/{kommentarId}")
    public ResponseEntity<RequestStatusResponseDTO> deleteKommentar(
            @Parameter(
                    description = "UUID of the comment",
                    required = true)
            @PathVariable final UUID kommentarId,
            final Authentication authentication) {
        kommentarService.deleteKommentar(
            kommentarId, authentication.getName());
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .body(new RequestStatusResponseDTO(
                "Kommentar deleted successfully.",
                PersistenceOperation.DELETE));
    }
}
