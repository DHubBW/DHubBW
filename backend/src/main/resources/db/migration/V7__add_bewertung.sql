-- V7: Add per-user star ratings for files plus the per-file aggregate.
-- The aggregate (sum + count) is held redundantly on the datei row so
-- list views never have to recompute the average. The unique constraint
-- prevents a single user from rating the same file twice.
ALTER TABLE datei
    ADD COLUMN IF NOT EXISTS bewertung_summe BIGINT NOT NULL DEFAULT 0;
ALTER TABLE datei
    ADD COLUMN IF NOT EXISTS bewertung_anzahl INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS bewertung (
    bewertung_id UUID    NOT NULL PRIMARY KEY,
    datei_id     UUID    REFERENCES datei (datei_id),
    autor_id     UUID    REFERENCES student (student_id),
    sterne       INT     NOT NULL,
    bewertet_am  TIMESTAMP,
    CONSTRAINT uk_bewertung_datei_autor UNIQUE (datei_id, autor_id)
);
