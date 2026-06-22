package com.se.dhubbw.shared.infrastructure.backup;

import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily backup scheduler.
 *
 * <p>Triggers an automatic backup once per day at 03:00 (server
 * time). The cron expression is intentionally kept on a quiet,
 * off-peak window so the snapshot does not race with normal
 * upload / download traffic.</p>
 *
 * <p>Disabled outside of {@code prod}-like profiles via the
 * {@link Scheduled} expression — there's no business reason to
 * run the daily snapshot on every developer machine. The manual
 * backup endpoint exposed by the controller still works
 * everywhere for ad-hoc triggers.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Scheduling component)
 */
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    /**
     * Service that captures and stores the snapshot.
     */
    private final BackupService backupService;

    /**
     * Color-coded logger for status output.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(BackupScheduler.class);

    /**
     * Cron entry-point — daily at 03:00 server time.
     */
    @Scheduled(cron = "${dhubbw.backup.cron:0 0 3 * * *}")
    public void runDailyBackup() {
        try {
            BackupDTO dto = backupService.createBackup("AUTO");
            colorLogger.colorInfo(
                "Daily backup {} done at {}",
                dto.backupID(), dto.erstelltAm());
        } catch (RuntimeException ex) {
            colorLogger.colorError(
                "Daily backup failed: {}",
                ex.getMessage(), ex);
            // Even when the snapshot itself failed we still want
            // expired rows pruned, so call the retention sweep
            // explicitly.
            try {
                backupService.pruneExpired();
            } catch (RuntimeException pex) {
                colorLogger.colorError(
                    "Backup retention sweep failed: {}",
                    pex.getMessage(), pex);
            }
        }
    }
}
