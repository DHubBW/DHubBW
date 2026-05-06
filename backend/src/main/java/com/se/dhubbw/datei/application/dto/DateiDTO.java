package com.se.dhubbw.datei.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object for file metadata.
 *
 * <p>Carries display-relevant metadata only; the binary file content
 * is intentionally excluded to keep the response lightweight.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DateiDTO {
    /**
     * Unique file identifier.
     */
    private UUID dateiID;

    /**
     * File name.
     */
    private String name;

    /**
     * Date the file was last modified.
     */
    private LocalDate zuletztBearbeitet;

    /**
     * File size in megabytes.
     */
    private float fileSize;

    /**
     * Full name of the student who uploaded the file,
     * or {@code null} if no uploader is recorded.
     */
    private String uploaderName;

    /**
     * Identifier of the folder containing this file.
     */
    private UUID ordnerID;

    /**
     * Name of the folder containing this file.
     */
    private String ordnerName;
}
