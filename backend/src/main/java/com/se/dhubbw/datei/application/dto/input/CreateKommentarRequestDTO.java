package com.se.dhubbw.datei.application.dto.input;

/**
 * Request body for creating a comment on a file.
 *
 * <p>Used by {@code POST /student/dateien/{dateiId}/kommentare}.
 * The {@code inhalt} property carries the comment text; the
 * service layer rejects blank or oversize content.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 *
 * @param inhalt comment text body — must not be blank
 */
public record CreateKommentarRequestDTO(String inhalt) { }
