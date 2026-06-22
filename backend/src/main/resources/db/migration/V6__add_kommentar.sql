-- V6: Introduce the kommentar table for file comments.
-- A comment always references exactly one file and is authored by
-- exactly one student. The bearbeitet flag plus the
-- zuletzt_bearbeitet timestamp track whether and when the comment
-- has been edited after creation.
CREATE TABLE IF NOT EXISTS kommentar (
    kommentar_id        UUID         NOT NULL PRIMARY KEY,
    datei_id            UUID         REFERENCES datei (datei_id),
    autor_id            UUID         REFERENCES student (student_id),
    inhalt              VARCHAR(2000),
    erstellt_am         TIMESTAMP,
    zuletzt_bearbeitet  TIMESTAMP,
    bearbeitet          BOOLEAN      NOT NULL DEFAULT FALSE
);
