-- V9: Backup-Feature-Tabelle.
-- Tally-Felder werden redundant gespeichert, damit das Admin-UI
-- "was war im Snapshot" anzeigen kann ohne den JSON-Payload zu
-- parsen. Der payload selbst enthaelt die wiederherstellbaren
-- Daten (siehe BackupServiceImpl#captureSnapshotJson).
CREATE TABLE IF NOT EXISTS backup (
    backup_id        UUID         NOT NULL PRIMARY KEY,
    erstellt_am      TIMESTAMP    NOT NULL,
    quelle           VARCHAR(16)  NOT NULL,
    ordner_anzahl    INT          NOT NULL,
    datei_anzahl     INT          NOT NULL,
    kommentar_anzahl INT          NOT NULL,
    bewertung_anzahl INT          NOT NULL,
    tag_anzahl       INT          NOT NULL,
    payload          TEXT         NOT NULL
);
