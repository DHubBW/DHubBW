package com.se.dhubbw.ordner.application;

import com.se.dhubbw.ordner.application.dto.input.CreateOrdnerRequestDTO;
import com.se.dhubbw.ordner.application.dto.input.RenameOrdnerRequestDTO;
import com.se.dhubbw.ordner.application.dto.output.OrdnerDTO;
import com.se.dhubbw.ordner.domain.OrdnerService;
import com.se.dhubbw.shared.application.dto.output.SmallResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for folder operations (create, list, rename, delete).
 * Provides HTTP endpoints for folder management and delegates to
 * {@link OrdnerService}.
 *
 * <p>Provides endpoints for listing, creating, renaming, and
 * deleting folders. Permissions are enforced at the service
 * layer based on the caller's role and course membership.</p>
 *
 * @architecture.component Ordnerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category T-Software (REST endpoints with Spring Web
 *     annotations)
 * @see OrdnerService
 */
@RestController
@RequestMapping("/student/ordner")
@RequiredArgsConstructor
public class OrdnerController {

    /**
     * Service containing all folder management business logic.
     */
    private final OrdnerService ordnerService;

    /**
     * Retrieve all root-level folders for the caller's course.
     *
     * @param authentication the current user's security context
     * @return 200 with the list of root folders
     */
    @Operation(
        summary = "List root folders",
        description = "Returns all root-level folders of the "
            + "caller's course."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Root folders retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = OrdnerDTO.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Student or course not found",
        content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping
    public ResponseEntity<List<OrdnerDTO>> retrieveRootFolders(
            final Authentication authentication) {
        List<OrdnerDTO> folders =
            ordnerService.retrieveRootFolders(
                authentication.getName());
        return ResponseEntity.ok(folders);
    }

    /**
     * Retrieve the path structure from the root folder to the
     * given folder (inclusive, root first).
     *
     * @param ordnerId       the UUID of the target folder
     * @param authentication the current user's security context
     * @return 200 with the ordered path list
     */
    @Operation(
        summary = "Get path from root to folder",
        description = "Returns all ancestor folders from root "
            + "down to the given folder."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Path from root to folder retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = OrdnerDTO.class)
        )
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorised to access this folder",
        content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
        responseCode = "404",
        description = "Folder or student not found",
        content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{ordnerId}/upper")
    public ResponseEntity<List<OrdnerDTO>> retrieveAllUpperFolders(
            @Parameter(
                description = "UUID of the target folder",
                required = true)
            @PathVariable final UUID ordnerId,
            final Authentication authentication) {
        List<OrdnerDTO> path = ordnerService.retrieveAllUpperFolders(
            ordnerId, authentication.getName());
        return ResponseEntity.ok(path);
    }

    /**
     * Retrieve all direct child folders of the given folder.
     *
     * @param ordnerId       the UUID of the parent folder
     * @param authentication the current user's security context
     * @return 200 with the list of direct child folders
     */
    @Operation(
        summary = "List direct child folders",
        description = "Returns all direct child folders of the "
            + "given parent folder."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Child folders retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = OrdnerDTO.class)
        )
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorised to access this folder",
        content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
        responseCode = "404",
        description = "Folder or student not found",
        content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{ordnerId}/children")
    public ResponseEntity<List<OrdnerDTO>>
            retrieveAllDirectFolderChildren(
            @Parameter(
                description = "UUID of the parent folder",
                required = true)
            @PathVariable final UUID ordnerId,
            final Authentication authentication) {
        List<OrdnerDTO> children =
            ordnerService.retrieveAllDirectFolderChildren(
                ordnerId, authentication.getName());
        return ResponseEntity.ok(children);
    }

    /**
     * Create a new folder.
     *
     * <p>If {@code ueberOrdnerId} is omitted in the request body,
     * a root-level folder is created.</p>
     *
     * @param request        the folder creation request body
     * @param authentication the current user's security context
     * @return 201 with the UUID of the created folder
     */
    @Operation(
        summary = "Create a folder",
        description = "Creates a new folder optionally nested "
            + "under a parent folder."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Folder created successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = UUID.class)
        )
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorised to create a folder here",
        content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
        responseCode = "404",
        description = "Parent folder, student or course not found",
        content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping
    public ResponseEntity<UUID> createFolder(
            @RequestBody
            final CreateOrdnerRequestDTO request,
            final Authentication authentication) {
        UUID id = ordnerService.createFolder(
            request.ueberOrdnerId(), request.name(),
            request.isPublic(), request.farbe(),
            authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    /**
     * Rename an existing folder.
     *
     * @param ordnerId       the UUID of the folder to rename
     * @param request        the rename request body
     * @param authentication the current user's security context
     * @return 200 if renamed successfully
     */
    @Operation(
        summary = "Rename a folder",
        description = "Updates the name of an existing folder. "
            + "Requires KURSADMIN, SYSTEMADMIN, or creator role."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Folder renamed successfully"
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorised to rename this folder",
        content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
        responseCode = "404",
        description = "Folder or student not found",
        content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping("/{ordnerId}")
    public ResponseEntity<SmallResponseDTO> renameFolder(
            @Parameter(
                description = "UUID of the folder to rename",
                required = true)
            @PathVariable final UUID ordnerId,
            @RequestBody
            final RenameOrdnerRequestDTO request,
            final Authentication authentication) {
        ordnerService.renameFolder(
            ordnerId, request.newName(),
            authentication.getName());
        return ResponseEntity.status(HttpStatus.OK)
            .body(new SmallResponseDTO("Folder renamed successfully"));
    }

    /**
     * Delete a folder and all of its contents recursively.
     *
     * @param ordnerId       the UUID of the folder to delete
     * @param authentication the current user's security context
     * @return 204 if deleted successfully
     */
    @Operation(
        summary = "Delete a folder",
        description = "Permanently deletes a folder and all "
            + "of its child folders and files. Requires "
            + "KURSADMIN, SYSTEMADMIN, or creator role."
    )
    @ApiResponse(
        responseCode = "204",
        description = "Folder deleted successfully"
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorised to delete this folder",
        content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
        responseCode = "404",
        description = "Folder or student not found",
        content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/{ordnerId}")
    public ResponseEntity<SmallResponseDTO> deleteOrdner(
            @Parameter(
                description = "UUID of the folder to delete",
                required = true)
            @PathVariable final UUID ordnerId,
            final Authentication authentication) {
        ordnerService.deleteOrdner(
            ordnerId, authentication.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(new SmallResponseDTO("Folder deleted successfully"));
    }
}
