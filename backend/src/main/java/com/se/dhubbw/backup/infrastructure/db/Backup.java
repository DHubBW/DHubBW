package com.se.dhubbw.backup.infrastructure.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a single automatic file-management backup.
 *
 * <p>Each backup row carries a JSON payload of the file-management
 * domain (folders, files metadata, tags, comments, ratings) plus
 * tally fields used by the admin UI to surface "what was in this
 * snapshot" without re-parsing the JSON. Binary file content is not
 * included — restoring metadata is sufficient to re-establish the
 * navigation tree, comment threads, ratings and tag assignments.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data persistence layer)
 * @architecture.category T-Software (JPA entity mapping)
 */
@Entity
@Table(name = "backup")
@Getter
@Setter
public class Backup {

    /**
     * Maximum length of the {@code quelle} discriminator column.
     * Mirrors the value used in the V9 Flyway migration.
     */
    public static final int QUELLE_MAX_LENGTH = 16;

    /**
     * Unique backup identifier.
     */
    @Id
    @Column(name = "backup_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID backupID;

    /**
     * Timestamp the backup was created.
     */
    @Column(name = "erstellt_am", nullable = false)
    private LocalDateTime erstelltAm;

    /**
     * Source of the backup run — {@code "AUTO"} for the daily
     * scheduler, {@code "MANUAL"} for an admin-triggered run.
     */
    @Column(name = "quelle", nullable = false, length = QUELLE_MAX_LENGTH)
    private String quelle;

    /** Number of {@code ordner} rows in the snapshot. */
    @Column(name = "ordner_anzahl", nullable = false)
    private int ordnerAnzahl;

    /** Number of {@code datei} rows in the snapshot. */
    @Column(name = "datei_anzahl", nullable = false)
    private int dateiAnzahl;

    /** Number of {@code kommentar} rows in the snapshot. */
    @Column(name = "kommentar_anzahl", nullable = false)
    private int kommentarAnzahl;

    /** Number of {@code bewertung} rows in the snapshot. */
    @Column(name = "bewertung_anzahl", nullable = false)
    private int bewertungAnzahl;

    /** Number of {@code tag} rows in the snapshot. */
    @Column(name = "tag_anzahl", nullable = false)
    private int tagAnzahl;

    /**
     * JSON serialisation of the snapshot. The structure is internal
     * and only consumed by the matching restore endpoint — see
     * {@code BackupServiceImpl#captureSnapshotJson}.
     */
    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;
}
