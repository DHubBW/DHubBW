package com.se.dhubbw.datei.application.dto.output;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data transfer object representing a comment on a file.
 *
 * <p>Carries metadata for display in the file's comment thread:
 * author identification, the textual content, both creation and
 * last-edit timestamps, and an explicit flag signalling whether the
 * comment has been edited.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 *
 * @param kommentarID Unique comment identifier.
 * @param dateiID Identifier of the commented file.
 * @param autorID Identifier of the authoring student, or
 *     {@code null} if the author has been removed.
 * @param autorName Display name of the authoring student, or
 *     {@code null} if the author has been removed.
 * @param inhalt The comment text body.
 * @param erstelltAm Timestamp the comment was originally created.
 * @param zuletztBearbeitet Timestamp of the last edit, or
 *     {@code null} if the comment has never been edited.
 * @param bearbeitet {@code true} if the comment has been edited at
 *     least once after creation.
 * @param canEdit {@code true} if the requesting student is allowed
 *     to edit this comment.
 * @param canDelete {@code true} if the requesting student is allowed
 *     to delete this comment.
 */
public record KommentarDTO(UUID kommentarID,
                           UUID dateiID,
                           UUID autorID,
                           String autorName,
                           String inhalt,
                           LocalDateTime erstelltAm,
                           LocalDateTime zuletztBearbeitet,
                           boolean bearbeitet,
                           boolean canEdit,
                           boolean canDelete) { }
