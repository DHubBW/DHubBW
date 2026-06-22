package com.se.dhubbw.shared.infrastructure.backup;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for the SYSTEMADMIN backup endpoints that need a
 * two-fold confirmation:
 *
 * <ul>
 *   <li>{@code POST /systemadmin/backups/{id}/restore}</li>
 *   <li>{@code DELETE /systemadmin/backups/{id}}</li>
 * </ul>
 *
 * <p>The {@code confirmationCount} field is checked against
 * {@link BackupServiceImpl#MIN_CONFIRMATION_COUNT} so even a hand-
 * rolled HTTP call cannot bypass the double-confirm rule.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RestoreBackupRequestDTO {

    /**
     * Number of confirmations the admin has clicked through. Must
     * be at least {@link BackupServiceImpl#MIN_CONFIRMATION_COUNT}
     * for the action (restore or delete) to actually run.
     */
    private int confirmationCount;
}
