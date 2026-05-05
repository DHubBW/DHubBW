-- Initial schema – mirrors current JPA entities

CREATE TABLE student (
    student_id    UUID         NOT NULL PRIMARY KEY,
    student_type  VARCHAR(255),
    name          VARCHAR(255),
    password_hash VARCHAR(255),
    dhbw_mail     VARCHAR(255)
);

CREATE TABLE kurs (
    kurs_id   UUID         NOT NULL PRIMARY KEY,
    kurs_name VARCHAR(255),
    jahrgang  VARCHAR(255)
);

CREATE TABLE mensa (
    gericht_id        UUID           NOT NULL PRIMARY KEY,
    datum             DATE,
    essensbezeichnung VARCHAR(255),
    allergene         VARCHAR(255),
    preis             DOUBLE PRECISION,
    ernaehrung        VARCHAR(255)
);

CREATE TABLE ordner (
    ordner_id       UUID    NOT NULL PRIMARY KEY,
    ueber_ordner_id UUID    REFERENCES ordner (ordner_id),
    kurs_id         UUID    REFERENCES kurs (kurs_id),
    name            VARCHAR(255),
    level           INTEGER NOT NULL DEFAULT 0,
    farbe           INTEGER,
    is_public       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE datei (
    datei_id           UUID NOT NULL PRIMARY KEY,
    ordner_id          UUID REFERENCES ordner (ordner_id),
    name               VARCHAR(255),
    zuletzt_bearbeitet DATE,
    data               BYTEA,
    uploader           UUID REFERENCES student (student_id),
    file_size          REAL
);


CREATE TABLE kurs_beitritts_anfrage (
    anfrage_id  UUID    NOT NULL PRIMARY KEY,
    kurs_id     UUID    REFERENCES kurs (kurs_id),
    student_id  UUID    REFERENCES student (student_id),
    datum       DATE,
    is_approved BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE kurs_erstellungs_anfrage (
    anfrage_id   UUID    NOT NULL PRIMARY KEY,
    student_id   UUID    REFERENCES student (student_id),
    kurs_name    VARCHAR(255),
    jahrgang     VARCHAR(255),
    anfrage_datum DATE,
    is_approved  BOOLEAN NOT NULL DEFAULT FALSE,
    request_notes VARCHAR(255) DEFAULT 'No notes were provided.'
);

CREATE TABLE datei_anlegung (
    anlegung_id UUID NOT NULL PRIMARY KEY,
    student_id  UUID REFERENCES student (student_id),
    datei_id    UUID REFERENCES datei (datei_id),
    datum       DATE
);

CREATE TABLE ordner_anlegung (
    anlegung_id UUID NOT NULL PRIMARY KEY,
    student_id  UUID REFERENCES student (student_id),
    ordner_id   UUID REFERENCES ordner (ordner_id),
    datum       DATE
);
