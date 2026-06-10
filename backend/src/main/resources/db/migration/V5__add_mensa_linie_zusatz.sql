-- V5: Add the `linie` and `zusatz` columns to the mensa table.
-- The `linie` column was introduced on the entity earlier (serving line
-- like "Wahlessen 1") but never made it into a migration.
-- The `zusatz` column carries the descriptive text shown next to the
-- meal title on sw-ka.de (e.g. "mit Pfannkuchen oder Dampfkartoffeln
-- und Soße nach holländischer Art oder Butter").
-- IF NOT EXISTS keeps the migration idempotent on databases where the
-- columns were silently created by a previous Hibernate auto-DDL run.
ALTER TABLE mensa ADD COLUMN IF NOT EXISTS linie VARCHAR(255);
ALTER TABLE mensa ADD COLUMN IF NOT EXISTS zusatz VARCHAR(1000);
