-- Students
-- note: test data students use the same string for their username and password (bcrypt encoded)
INSERT INTO student (student_id, student_type, name, password_hash, dhbw_mail) VALUES
    ('11111111-1111-1111-1111-111111111111', 'STUDENT', 'Linus', '$2a$10$hIJrX3Gm2GvsdG.RCk8d4uI3lqdS3OhcohSa/rsMQzCgcBckrnKom', 'linus.kronenberger@dhbw-karlsruhe.de'),
    ('11111111-1111-1111-1111-111111111112', 'KURSADMIN', 'Nico', '$2a$10$EU3zuRnSvnl4m/sX8AWiCeILm.oKvm6Y5ySuq.Ycvp84tmIxxnYz6', 'nico.anonym@dhbw-karlsruhe.de'),
    ('11111111-1111-1111-1111-111111111113', 'SYSTEMADMIN', 'Tom', '$2a$10$B0cbDwnXAoS0PQLSc8tiYeGXXZi25wtMqQTQ0KRCSZjCPC2QodlJe', 'tom.random@dhbw-karlsruhe.de');

-- Kurse
INSERT INTO kurs (kurs_id, kurs_name, jahrgang) VALUES
    ('22222222-2222-2222-2222-222222222221', 'TINF24B2', '2024'),
    ('22222222-2222-2222-2222-222222222222', 'TINF24B1', '2024');

-- Mensa-Gerichte
INSERT INTO mensa (gericht_id, datum, essensbezeichnung, allergene, preis, ernaehrung) VALUES
    ('33333333-3333-3333-3333-333333333331', DATE '2026-05-02', 'Spaghetti Bolognese', 'Gluten,Eier', 4.50, 'FLEISCH'),
    ('33333333-3333-3333-3333-333333333332', DATE '2026-05-02', 'Gemueselasagne', 'Gluten,Milch', 4.20, 'VEGETARISCH'),
    ('33333333-3333-3333-3333-333333333333', DATE '2026-05-03', 'Curry mit Tofu', 'Soja', 4.80, 'VEGAN');

-- Ordner (parent first, then child for self-reference)
INSERT INTO ordner (ordner_id, ueber_ordner_id, kurs_id, name, level, farbe, is_public) VALUES
    ('44444444-4444-4444-4444-444444444441', NULL, '22222222-2222-2222-2222-222222222221', 'Vorlesungen', 0, 0, TRUE),
    ('44444444-4444-4444-4444-444444444442', '44444444-4444-4444-4444-444444444441', '22222222-2222-2222-2222-222222222221', 'Mathematik', 1, 1, TRUE),
    ('44444444-4444-4444-4444-444444444443', NULL, '22222222-2222-2222-2222-222222222222', 'Privater Ordner', 0, 2, FALSE);

-- Dateien
-- data column is loaded from src/main/resources/test-images/sample.png via H2's FILE_READ
INSERT INTO datei (datei_id, ordner_id, name, zuletzt_bearbeitet, data, uploader, file_size) VALUES
    ('55555555-5555-5555-5555-555555555551', '44444444-4444-4444-4444-444444444442', 'Skript_Mathe.pdf', DATE '2026-04-25', FILE_READ('classpath:/test-images/sample.png'), '11111111-1111-1111-1111-111111111112', 2.5),
    ('55555555-5555-5555-5555-555555555552', '44444444-4444-4444-4444-444444444443', 'Notizen.txt', DATE '2026-04-30', FILE_READ('classpath:/test-images/sample.png'), '11111111-1111-1111-1111-111111111111', 0.8);

-- Kurs-Beitrittsanfragen
INSERT INTO kurs_beitritts_anfrage (anfrage_id, kurs_id, student_id, datum, is_approved) VALUES
    ('66666666-6666-6666-6666-666666666661', '22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', DATE '2026-04-15', TRUE),
    ('66666666-6666-6666-6666-666666666662', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111112', DATE '2026-04-20', FALSE);

-- Kurs-Erstellungsanfragen
INSERT INTO kurs_erstellungs_anfrage (anfrage_id, student_id, kurs_name, jahrgang, anfrage_datum, is_approved, request_notes) VALUES
    ('99999999-9999-9999-9999-999999999991', '11111111-1111-1111-1111-111111111112', 'TINF24B3', '2024', DATE '2026-04-18', TRUE, 'Bitte lass mich Kurs erstellen!'),
    ('99999999-9999-9999-9999-999999999992', '11111111-1111-1111-1111-111111111111', 'WINF25A1', '2025', DATE '2026-04-22', FALSE, 'Ich bin sehr unzufrieden ohne Kurs!');

-- Ordner-Anlegungen
INSERT INTO ordner_anlegung (anlegung_id, student_id, ordner_id, datum) VALUES
    ('77777777-7777-7777-7777-777777777771', '11111111-1111-1111-1111-111111111112', '44444444-4444-4444-4444-444444444441', DATE '2026-04-01'),
    ('77777777-7777-7777-7777-777777777772', '11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444443', DATE '2026-04-10');

-- Datei-Anlegungen
INSERT INTO datei_anlegung (anlegung_id, student_id, datei_id, datum) VALUES
    ('88888888-8888-8888-8888-888888888881', '11111111-1111-1111-1111-111111111112', '55555555-5555-5555-5555-555555555551', DATE '2026-04-25'),
    ('88888888-8888-8888-8888-888888888882', '11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555555552', DATE '2026-04-30');