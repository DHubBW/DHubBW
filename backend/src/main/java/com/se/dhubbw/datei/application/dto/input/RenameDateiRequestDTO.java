package com.se.dhubbw.datei.application.dto.input;

/**
 * Request body for renaming a file.
 *
 * @param newName new display name for the file
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 */
public record RenameDateiRequestDTO(String newName) { }
