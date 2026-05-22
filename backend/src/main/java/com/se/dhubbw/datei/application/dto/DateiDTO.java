package com.se.dhubbw.datei.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object for file metadata.
 *
 * <p>Carries display-relevant metadata only; the binary file content
 * is intentionally excluded to keep the response lightweight.</p>
 * @param dateiID Unique file identifier.
 * @param name File name.
 * @param zuletztBearbeitet Date the file was last modified.
 * @param fileSize File size in megabytes.
 * @param uploaderName Full name of the student who uploaded the file,
 * or {@code null} if no uploader is recorded.
 * @param ordnerID Identifier of the folder containing this file.
 * @param ordnerName Name of the folder containing this file.
 */
public record DateiDTO(UUID dateiID,
                       String name,
                       LocalDate zuletztBearbeitet,
                       float fileSize,
                       String uploaderName,
                       UUID ordnerID,
                       String ordnerName) { }
