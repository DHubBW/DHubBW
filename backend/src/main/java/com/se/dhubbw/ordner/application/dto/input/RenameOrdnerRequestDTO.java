package com.se.dhubbw.ordner.application.dto.input;

/**
 * Request body for renaming a folder.
 *
 * @param newName new display name for the folder
 * @architecture.component Ordnerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 */
public record RenameOrdnerRequestDTO(String newName) { }
