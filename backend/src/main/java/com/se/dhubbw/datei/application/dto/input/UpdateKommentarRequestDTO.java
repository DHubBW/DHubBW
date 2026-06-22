package com.se.dhubbw.datei.application.dto.input;

/**
 * Request body for editing an existing file comment.
 *
 * <p>Used by {@code PATCH /student/kommentare/{kommentarId}}. Only
 * the comment author may edit; on success the comment's
 * {@code bearbeitet} flag flips to {@code true} and the modification
 * timestamp is recorded.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 *
 * @param inhalt new comment text body — must not be blank
 */
public record UpdateKommentarRequestDTO(String inhalt) { }
