package com.se.dhubbw.backup.application.dto.input;

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
 * {@code BackupServiceImpl#MIN_CONFIRMATION_COUNT} so even a
 * hand-rolled HTTP call cannot bypass the double-confirm rule.</p>
 *
 * @param confirmationCount number of confirmations the admin has
 *     clicked through
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 */
public record RestoreBackupRequestDTO(int confirmationCount) { }
