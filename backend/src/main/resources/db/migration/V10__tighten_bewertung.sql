-- V10: Tighten the bewertung table after the V7-baseline review.
-- The Bewertung entity was updated to declare datei_id, autor_id
-- and bewertet_am as NOT NULL plus the UNIQUE constraint was given
-- a more readable name. The original V7 migration shipped with
-- nullable columns and a less explicit constraint label; this
-- migration brings the live schema in line with the entity now
-- that the service layer always sets all three values.
ALTER TABLE bewertung ALTER COLUMN datei_id SET NOT NULL;
ALTER TABLE bewertung ALTER COLUMN autor_id SET NOT NULL;
ALTER TABLE bewertung ALTER COLUMN bewertet_am SET NOT NULL;

ALTER TABLE bewertung DROP CONSTRAINT IF EXISTS uk_bewertung_datei_autor;
ALTER TABLE bewertung
    ADD CONSTRAINT uk_bewertung_one_per_file_per_author
    UNIQUE (datei_id, autor_id);
