package com.se.dhubbw.datei.application.dto.input;

/**
 * Request body for {@code PUT /student/dateien/{dateiId}/bewertung}.
 *
 * <p>Used to create or replace the caller's star rating on a file.
 * Half-stars are not supported — {@code sterne} must be a whole
 * number between 1 and 5 (inclusive).</p>
 *
 * <p>Naming follows the {@code Create*RequestDTO} convention used by
 * the rest of the module ({@link CreateKommentarRequestDTO},
 * {@link CreateTagRequestDTO}). The HTTP verb is {@code PUT} because
 * the request is idempotent on the {@code (datei, autor)} unique
 * pair: the server inserts a new row when the caller has not yet
 * rated the file and updates the existing row otherwise.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 *
 * @param sterne star count between 1 and 5 (inclusive)
 */
public record CreateBewertungRequestDTO(int sterne) { }
