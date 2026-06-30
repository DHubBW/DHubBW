package com.se.dhubbw.backup.application.dto.output;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO summarising a single backup run.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     for API response)
 *
 * @param backupID         Unique backup identifier.
 * @param erstelltAm       Timestamp the backup was created.
 * @param quelle           Source of the run — {@code "AUTO"} or
 *     {@code "MANUAL"}.
 * @param ordnerAnzahl     Number of folders in the snapshot.
 * @param dateiAnzahl      Number of files in the snapshot.
 * @param kommentarAnzahl  Number of comments in the snapshot.
 * @param bewertungAnzahl  Number of ratings in the snapshot.
 * @param tagAnzahl        Number of tags in the snapshot.
 */
public record BackupDTO(UUID backupID,
                        LocalDateTime erstelltAm,
                        String quelle,
                        int ordnerAnzahl,
                        int dateiAnzahl,
                        int kommentarAnzahl,
                        int bewertungAnzahl,
                        int tagAnzahl) { }
