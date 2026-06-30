package com.se.dhubbw.datei.application.dto.output;

/**
 * Data transfer object for file download responses.
 * Carries the binary content together with the file name for the HTTP response.
 *
 * <p>Carries the binary file content together with its display
 * name so the controller can build the correct
 * Content-Disposition and Content-Type headers.</p>
 *
 * @param name file display name (used for Content-Disposition)
 * @param data raw file bytes
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
public record DateiDownloadDTO(String name, byte[] data) { }
