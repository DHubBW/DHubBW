-- V8: Tags und N:M-Verknuepfung Datei <-> Tag.
CREATE TABLE IF NOT EXISTS tag (
    tag_id        UUID         NOT NULL PRIMARY KEY,
    name          VARCHAR(64)  NOT NULL UNIQUE,
    ersteller_id  UUID         REFERENCES student (student_id),
    erstellt_am   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS datei_tag (
    datei_id UUID NOT NULL REFERENCES datei (datei_id),
    tag_id   UUID NOT NULL REFERENCES tag (tag_id),
    PRIMARY KEY (datei_id, tag_id)
);
