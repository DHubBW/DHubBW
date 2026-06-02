-- Add download counter column to datei table.
ALTER TABLE datei ADD COLUMN download_anzahl INT NOT NULL DEFAULT 0;
