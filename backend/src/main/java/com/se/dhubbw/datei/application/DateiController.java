package com.se.dhubbw.datei.application;

import com.se.dhubbw.datei.application.dto.DateiDTO;
import com.se.dhubbw.datei.application.dto.DateiDownloadDTO;
import com.se.dhubbw.datei.application.dto.DateiUploadDTO;
import com.se.dhubbw.datei.domain.DateiService;
import com.se.dhubbw.shared.application.dto.RequestStatusResponseDTO;
import com.se.dhubbw.shared.application.dto.util.PersistenceOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for file management endpoints.
 *
 * <p>Provides endpoints for uploading, renaming, and deleting
 * files. Permissions are enforced at the service layer based on
 * the caller's role and course membership.</p>
 */
@RestController
@RequestMapping("/dateien")
@RequiredArgsConstructor
public class DateiController {

    /**
     * Service containing all file management business logic.
     */
    private final DateiService dateiService;

    /**
     * Upload a file to a folder.
     *
     * <p>Requires at least the {@code STUDENT} role. The caller
     * must be a member of the target folder's course, or
     * SYSTEMADMIN. The platform storage limit must not be
     * exceeded.</p>
     *
     * @param file          the multipart file to upload
     * @param ueberordnerId the UUID of the target folder
     * @param name          the display name for the file
     * @param authentication the current user's security context
     * @return 201 with the UUID of the created file
     */
    @Operation(
            summary = "Upload a file",
            description = "Stores a file in the given folder. "
                + "Caller must be a member of the folder's "
                + "course or SYSTEMADMIN."
    )
    @ApiResponse(
            responseCode = "201",
            description = "File uploaded successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UUID.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorised to upload to this folder",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "Folder or student not found",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "507",
            description = "Platform storage limit exceeded",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DateiUploadDTO> uploadDatei(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") final MultipartFile file,
            @Parameter(
                    description = "UUID of the target folder",
                    required = true)
            @RequestParam("ueberordnerId") final UUID ueberordnerId,
            @Parameter(
                    description = "Display name for the file",
                    required = true)
            @RequestParam("name") final String name,
            final Authentication authentication) {
        UUID dateiId = dateiService.uploadDatei(
            file,
            ueberordnerId,
            name,
            authentication.getName());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new DateiUploadDTO(dateiId));
    }

    /**
     * Retrieve all files in a folder.
     *
     * <p>Requires at least the {@code STUDENT} role. The caller
     * must be a member of the folder's course, or
     * SYSTEMADMIN.</p>
     *
     * @param ordnerId       the UUID of the target folder
     * @param authentication the current user's security context
     * @return 200 with the list of file metadata DTOs
     */
    @Operation(
            summary = "List files in a folder",
            description = "Returns file metadata for all files "
                + "in the given folder. Caller must be a "
                + "member of the folder's course or SYSTEMADMIN."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Files retrieved successfully"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorised to view this folder",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "Folder or student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/ordner/{ordnerId}")
    public ResponseEntity<List<DateiDTO>> retrieveDateienForOrdner(
            @Parameter(
                    description = "UUID of the target folder",
                    required = true)
            @PathVariable final UUID ordnerId,
            final Authentication authentication) {
        List<DateiDTO> dateien = dateiService
            .retrieveDateienForOrdner(
                ordnerId,
                authentication.getName());
        return ResponseEntity.ok(dateien);
    }

    /**
     * Retrieve all files of the caller's course.
     *
     * <p>Requires at least the {@code STUDENT} role. Only files
     * belonging to the authenticated user's own course are
     * returned. SYSTEMADMIN receives all files across all
     * courses.</p>
     *
     * @param authentication the current user's security context
     * @return 200 with the list of file metadata DTOs
     */
    @Operation(
            summary = "List all files for the current course",
            description = "Returns file metadata for all files "
                + "in the caller's course. SYSTEMADMIN sees "
                + "all files across all courses."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Files retrieved successfully"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping
    public ResponseEntity<List<DateiDTO>> retrieveAllFiles(
            final Authentication authentication) {
        List<DateiDTO> dateien = dateiService
            .retrieveAllFiles(authentication.getName());
        return ResponseEntity.ok(dateien);
    }

    /**
     * Download a file.
     *
     * <p>Requires at least the {@code STUDENT} role. The caller
     * must be a member of the file's course, or SYSTEMADMIN. If
     * the folder has no course, the restriction is waived.</p>
     *
     * @param dateiId        the UUID of the file to download
     * @param authentication the current user's security context
     * @return 200 with the file bytes, correct Content-Type and
     *     Content-Disposition header
     */
    @Operation(
            summary = "Download a file",
            description = "Streams a file's raw bytes with the "
                + "correct Content-Type and Content-Disposition "
                + "headers. Caller must be a member of the "
                + "file's course or SYSTEMADMIN."
    )
    @ApiResponse(
            responseCode = "200",
            description = "File content"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorised to download this file",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "File or student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{dateiId}/download")
    public ResponseEntity<Resource> downloadDatei(
            @Parameter(
                    description = "UUID of the file to download",
                    required = true)
            @PathVariable final UUID dateiId,
            final Authentication authentication) {
        DateiDownloadDTO dto = dateiService.downloadDatei(
            dateiId, authentication.getName());
        MediaType contentType = MediaTypeFactory
            .getMediaType(dto.getName())
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
            .contentType(contentType)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\""
                    + dto.getName() + "\"")
            .body(new InputStreamResource(
                new ByteArrayInputStream(dto.getData())));
    }

    /**
     * Rename a file.
     *
     * <p>Requires at least the {@code STUDENT} role. The caller
     * must be a member of the file's course or SYSTEMADMIN. If
     * the folder has no course, the restriction is waived.</p>
     *
     * @param dateiId        the UUID of the file to rename
     * @param newName        the new display name
     * @param authentication the current user's security context
     * @return 200 if renamed successfully
     */
    @Operation(
            summary = "Rename a file",
            description = "Updates the display name of a file. "
                + "Caller must be a member of the file's course "
                + "or SYSTEMADMIN."
    )
    @ApiResponse(
            responseCode = "200",
            description = "File renamed successfully"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorised to rename this file",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "File or student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping("/{dateiId}")
    public ResponseEntity<RequestStatusResponseDTO> renameDatei(
            @Parameter(
                    description = "UUID of the file to rename",
                    required = true)
            @PathVariable final UUID dateiId,
            @Parameter(
                    description = "New display name",
                    required = true)
            @RequestParam("newName") final String newName,
            final Authentication authentication) {
        dateiService.renameDatei(
            dateiId, newName, authentication.getName());
        return ResponseEntity
            .status(HttpStatus.OK).body(new RequestStatusResponseDTO(
            "Datei renamed successfully.", PersistenceOperation.UPDATE));
    }

    /**
     * Delete a file.
     *
     * <p>Requires at least the {@code STUDENT} role. The caller
     * must be a member of the file's course or SYSTEMADMIN. If
     * the folder has no course, the restriction is waived.</p>
     *
     * @param dateiId        the UUID of the file to delete
     * @param authentication the current user's security context
     * @return 204 if deleted successfully
     */
    @Operation(
            summary = "Delete a file",
            description = "Permanently deletes a file. "
                + "Caller must be a member of the file's course "
                + "or SYSTEMADMIN."
    )
    @ApiResponse(
            responseCode = "204",
            description = "File deleted successfully"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorised to delete this file",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "File or student not found",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/{dateiId}")
    public ResponseEntity<RequestStatusResponseDTO> deleteDatei(
            @Parameter(
                    description = "UUID of the file to delete",
                    required = true)
            @PathVariable final UUID dateiId,
            final Authentication authentication) {
        dateiService.deleteDatei(dateiId, authentication.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
            new RequestStatusResponseDTO("Datei deleted succesfully.",
                PersistenceOperation.DELETE));
    }
}
