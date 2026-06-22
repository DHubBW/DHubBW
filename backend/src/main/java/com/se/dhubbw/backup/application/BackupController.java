package com.se.dhubbw.backup.application;

import com.se.dhubbw.backup.application.dto.input.RestoreBackupRequestDTO;
import com.se.dhubbw.backup.application.dto.output.BackupDTO;
import com.se.dhubbw.backup.domain.BackupService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the file-management backup feature.
 *
 * <p>SYSTEMADMIN-only. Provides endpoints to trigger a manual
 * backup, list previous runs, retrieve the latest run, and restore
 * a specific backup with two-fold confirmation.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category T-Software (REST endpoints)
 * @see BackupService
 */
@RestController
@RequestMapping("/systemadmin/backups")
@RequiredArgsConstructor
public class BackupController {

    /**
     * Service containing the backup business logic.
     */
    private final BackupService backupService;

    /**
     * List all backups, newest first.
     *
     * @return 200 with the list of backup metadata
     */
    @Operation(
            summary = "List backups",
            description = "Returns all backups sorted newest first. "
                + "SYSTEMADMIN only."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Backups retrieved successfully"
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @GetMapping
    public ResponseEntity<List<BackupDTO>> listBackups() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    /**
     * Retrieve the most recent backup.
     *
     * @return 200 with the latest backup, or 204 when none exists
     */
    @Operation(
            summary = "Get the most recent backup",
            description = "SYSTEMADMIN only."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Latest backup metadata"
    )
    @ApiResponse(
            responseCode = "204",
            description = "No backup has been run yet"
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @GetMapping("/latest")
    public ResponseEntity<BackupDTO> latestBackup() {
        BackupDTO dto = backupService.latestBackup();
        if (dto == null) {
            return ResponseEntity.status(
                HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * Trigger a manual backup run.
     *
     * @return 201 with the created backup metadata
     */
    @Operation(
            summary = "Trigger a manual backup",
            description = "Creates a snapshot synchronously. "
                + "SYSTEMADMIN only."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Backup created successfully"
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @PostMapping
    public ResponseEntity<BackupDTO> createBackup() {
        BackupDTO created = backupService.createBackup("MANUAL");
        return ResponseEntity
            .status(HttpStatus.CREATED).body(created);
    }

    /**
     * Restore the given backup. Requires a two-fold confirmation
     * count.
     *
     * @param backupId the UUID of the backup to restore
     * @param request  the restore request body containing the
     *     {@code confirmationCount}
     * @return 200 if restored successfully
     */
    @Operation(
            summary = "Restore a backup",
            description = "Restores aggregate state from the given "
                + "snapshot. Requires confirmationCount >= 2 to "
                + "guard against accidental clicks. SYSTEMADMIN only."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Backup restored successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "confirmationCount is too low",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "No such backup",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @PostMapping("/{backupId}/restore")
    public ResponseEntity<Void> restoreBackup(
            @Parameter(
                    description = "UUID of the backup to restore",
                    required = true)
            @PathVariable final UUID backupId,
            @RequestBody final RestoreBackupRequestDTO request) {
        backupService.restoreBackup(
            backupId, request.getConfirmationCount());
        return ResponseEntity.ok().build();
    }

    /**
     * Manually delete a single backup. Like the restore endpoint
     * the call requires {@code confirmationCount >= 2} so a single
     * accidental click can never wipe a snapshot. The retention
     * sweep that auto-removes 14-day-old backups runs through a
     * separate code path that bypasses this gate.
     *
     * @param backupId the UUID of the backup to delete
     * @param request  the request body containing the
     *     {@code confirmationCount}
     * @return 200 if deleted successfully
     */
    @Operation(
            summary = "Delete a backup manually",
            description = "Removes a single backup. Requires "
                + "confirmationCount >= 2. Backups also auto-expire "
                + "after the configured retention window (default "
                + "14 days). SYSTEMADMIN only."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Backup deleted successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "confirmationCount is too low",
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
            responseCode = "404",
            description = "No such backup",
            content = @Content(mediaType = "application/json")
    )
    @PreAuthorize("hasRole('SYSTEMADMIN')")
    @DeleteMapping("/{backupId}")
    public ResponseEntity<RequestStatusResponseDTO> deleteBackup(
            @Parameter(
                    description = "UUID of the backup to delete",
                    required = true)
            @PathVariable final UUID backupId,
            @RequestBody final RestoreBackupRequestDTO request) {
        backupService.deleteBackup(
            backupId, request.getConfirmationCount());
        return ResponseEntity.ok(new RequestStatusResponseDTO(
            "Backup deleted successfully.",
            PersistenceOperation.DELETE));
    }
}
