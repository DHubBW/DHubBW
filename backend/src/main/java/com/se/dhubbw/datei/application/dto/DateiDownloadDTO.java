package com.se.dhubbw.datei.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for file download responses.
 *
 * <p>Carries the binary file content together with its display
 * name so the controller can build the correct
 * Content-Disposition and Content-Type headers.</p>
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
