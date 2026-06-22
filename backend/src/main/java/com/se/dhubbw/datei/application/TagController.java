package com.se.dhubbw.datei.application;

import com.se.dhubbw.datei.application.dto.input.CreateTagRequestDTO;
import com.se.dhubbw.datei.application.dto.output.TagDTO;
import com.se.dhubbw.datei.domain.TagService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for tags and per-file tag attachments.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code GET /student/tags} — list all tags (every authenticated
 *       student).</li>
 *   <li>{@code POST /student/tags} — create a new tag (KURSADMIN /
 *       SYSTEMADMIN only).</li>
 *   <li>{@code PUT /student/dateien/{dateiId}/tags/{tagId}} — attach a
 *       tag (uploader / KURSADMIN of folder / SYSTEMADMIN).</li>
 *   <li>{@code DELETE /student/dateien/{dateiId}/tags/{tagId}} — detach
 *       a tag (same authorisation as attach).</li>
 * </ul>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category T-Software (REST endpoints with Spring Web
 *     annotations)
 * @see TagService
 */
@RestController
@RequiredArgsConstructor
public class TagController {

    /**
     * Service containing all tag-management business logic.
     */
    private final TagService tagService;

    /**
     * List all tags, sorted alphabetically.
     *
     * @param authentication the current user's security context
     * @return 200 with the list of tag DTOs
     */
    @Operation(
            summary = "List all tags",
            description = "Returns all tags, sorted alphabetically."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tags retrieved successfully"
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/student/tags")
    public ResponseEntity<List<TagDTO>> retrieveAllTags(
            final Authentication authentication) {
        return ResponseEntity.ok(tagService.retrieveAllTags());
    }

    /**
     * Create a new tag (KURSADMIN / SYSTEMADMIN only).
     *
     * @param request        the create-tag request body
     * @param authentication the current user's security context
     * @return 201 with the created tag DTO
     */
    @Operation(
            summary = "Create a new tag",
            description = "Allowed for KURSADMIN and SYSTEMADMIN only."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Tag created successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Tag name is blank or too long",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "403",
            description = "Caller may not create tags",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "409",
            description = "A tag with the same name already exists",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/student/tags")
    public ResponseEntity<TagDTO> createTag(
            @RequestBody final CreateTagRequestDTO request,
            final Authentication authentication) {
        TagDTO created = tagService.createTag(
            request.name(), authentication.getName());
        return ResponseEntity
            .status(HttpStatus.CREATED).body(created);
    }

    /**
     * Attach an existing tag to a file.
     *
     * @param dateiId        the UUID of the file
     * @param tagId          the UUID of the tag
     * @param authentication the current user's security context
     * @return 200 if attached successfully
     */
    @Operation(
            summary = "Attach a tag to a file",
            description = "Allowed for the file's uploader, "
                + "KURSADMIN of the file's course, and SYSTEMADMIN."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tag attached successfully"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Caller may not modify this file's tags",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "File or tag not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/student/dateien/{dateiId}/tags/{tagId}")
    public ResponseEntity<RequestStatusResponseDTO> attachTag(
            @Parameter(
                    description = "UUID of the file",
                    required = true)
            @PathVariable final UUID dateiId,
            @Parameter(
                    description = "UUID of the tag",
                    required = true)
            @PathVariable final UUID tagId,
            final Authentication authentication) {
        tagService.attachTagToDatei(
            dateiId, tagId, authentication.getName());
        return ResponseEntity.ok(new RequestStatusResponseDTO(
            "Tag attached successfully.",
            PersistenceOperation.UPDATE));
    }

    /**
     * Detach a tag from a file.
     *
     * @param dateiId        the UUID of the file
     * @param tagId          the UUID of the tag
     * @param authentication the current user's security context
     * @return 200 if detached successfully
     */
    @Operation(
            summary = "Detach a tag from a file",
            description = "Allowed for the file's uploader, "
                + "KURSADMIN of the file's course, and SYSTEMADMIN."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tag detached successfully"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Caller may not modify this file's tags",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "File or tag not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/student/dateien/{dateiId}/tags/{tagId}")
    public ResponseEntity<RequestStatusResponseDTO> detachTag(
            @Parameter(
                    description = "UUID of the file",
                    required = true)
            @PathVariable final UUID dateiId,
            @Parameter(
                    description = "UUID of the tag",
                    required = true)
            @PathVariable final UUID tagId,
            final Authentication authentication) {
        tagService.detachTagFromDatei(
            dateiId, tagId, authentication.getName());
        return ResponseEntity.ok(new RequestStatusResponseDTO(
            "Tag detached successfully.",
            PersistenceOperation.UPDATE));
    }

    /**
     * Permanently delete a tag (SYSTEMADMIN only).
     *
     * @param tagId          the UUID of the tag to delete
     * @param authentication the current user's security context
     * @return 200 if deleted successfully
     */
    @Operation(
            summary = "Delete a tag globally",
            description = "Removes the tag from the platform and "
                + "detaches it from every file it was attached to. "
                + "SYSTEMADMIN only."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tag deleted successfully"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Caller is not SYSTEMADMIN",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "Tag not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @DeleteMapping("/student/tags/{tagId}")
    public ResponseEntity<RequestStatusResponseDTO> deleteTag(
            @Parameter(
                    description = "UUID of the tag to delete",
                    required = true)
            @PathVariable final UUID tagId,
            final Authentication authentication) {
        tagService.deleteTag(tagId, authentication.getName());
        return ResponseEntity.ok(new RequestStatusResponseDTO(
            "Tag deleted successfully.",
            PersistenceOperation.DELETE));
    }
}
