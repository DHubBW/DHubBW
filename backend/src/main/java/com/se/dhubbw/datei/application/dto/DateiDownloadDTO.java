package com.se.dhubbw.datei.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for file download responses.
 * Carries the binary content together with the file name for the HTTP response.
 *
 * <p>Carries the binary file content together with its display
 * name so the controller can build the correct
 * Content-Disposition and Content-Type headers.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DateiDownloadDTO {

    /**
     * File display name (used for Content-Disposition).
     */
    private String name;

    /**
     * Raw file bytes.
     */
    private byte[] data;
}
