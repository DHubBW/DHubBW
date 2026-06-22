package com.se.dhubbw.datei.application.dto.output;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Data transfer object for representing a file in API responses.
 * Contains metadata without binary data for the list view.
 *
 * <p>Carries display-relevant metadata only; the binary file content
 * is intentionally excluded to keep the response lightweight.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 * @param dateiID Unique file identifier.
 * @param name File name.
 * @param zuletztBearbeitet Date the file was last modified.
 * @param fileSize File size in megabytes.
 * @param uploaderName Full name of the student who uploaded the file,
 * or {@code null} if no uploader is recorded.
 * @param ordnerID Identifier of the folder containing this file.
 * @param ordnerName Name of the folder containing this file.
 * @param downloadAnzahl Number of times this file has been
 * downloaded.
 * @param bewertungDurchschnitt Average star rating across all
 * submitted votes, or {@code 0.0} when no rating exists yet.
 * @param bewertungAnzahl Total number of submitted votes.
 * @param tags Tags attached to this file (may be empty).
 */
public record DateiDTO(UUID dateiID,
                       String name,
                       LocalDate zuletztBearbeitet,
                       float fileSize,
                       String uploaderName,
                       UUID ordnerID,
                       String ordnerName,
                       int downloadAnzahl,
                       double bewertungDurchschnitt,
                       int bewertungAnzahl,
                       List<TagDTO> tags) { }
