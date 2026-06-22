package com.se.dhubbw.backup.domain;

import com.se.dhubbw.backup.application.dto.output.BackupDTO;

import java.util.List;
import java.util.UUID;

/**
 * Domain interface for the automatic file-management backup feature.
 *
 * <p>SYSTEMADMIN-only. Exposes:</p>
 * <ul>
 *   <li>{@link #createBackup(String)} — triggered by the scheduler
 *       and by the admin's manual button.</li>
 *   <li>{@link #listBackups()} — every backup, newest first.</li>
 *   <li>{@link #latestBackup()} — the most recent run, if any.</li>
 *   <li>{@link #restoreBackup(UUID, int)} — restore a specific
 *       backup, gated by a two-fold confirmation count.</li>
 * </ul>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface, technology-free)
 */
public interface BackupService {

    /**
     * Capture and persist a new backup.
     *
     * @param quelle source of the run — {@code "AUTO"} for the
     *     scheduler, {@code "MANUAL"} for the admin button
     * @return the created backup metadata
     */
    BackupDTO createBackup(String quelle);

    /**
     * Retrieve all backups, newest first.
     *
     * @return list of backup metadata DTOs
     */
    List<BackupDTO> listBackups();

    /**
     * Retrieve the most recent backup, if any.
     *
     * @return latest backup metadata, or {@code null} when no
     *     backup has ever been run
     */
    BackupDTO latestBackup();

    /**
     * Restore the given backup.
     *
     * <p>Two-fold confirmation is enforced: callers must pass a
     * {@code confirmationCount} of at least 2 — the admin UI uses
     * this to require an explicit double-confirm dialog.</p>
     *
     * @param backupId          UUID of the backup to restore
     * @param confirmationCount the caller's confirmation count
     *     (must be >= 2)
     */
    void restoreBackup(UUID backupId, int confirmationCount);

    /**
     * Delete a backup manually.
     *
     * <p>Like {@link #restoreBackup}, the deletion is gated by a
     * two-fold confirmation count so a single accidental click can
     * never wipe a snapshot. The retention sweep
     * ({@link #pruneExpired()}) bypasses this gate because it only
     * removes backups older than the configured retention window.</p>
     *
     * @param backupId          UUID of the backup to delete
     * @param confirmationCount the caller's confirmation count
     *     (must be >= 2)
     */
    void deleteBackup(UUID backupId, int confirmationCount);

    /**
     * Delete every backup that is older than the configured
     * retention window (default: 14 days).
     *
     * <p>Triggered by both the daily scheduler and after every
     * successful manual run, so the table cannot grow unbounded
     * without a separate ops job.</p>
     *
     * @return the number of pruned rows
     */
    int pruneExpired();
}
