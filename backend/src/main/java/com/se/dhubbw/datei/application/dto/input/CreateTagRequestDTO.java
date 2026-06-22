package com.se.dhubbw.datei.application.dto.input;

/**
 * Request body for {@code POST /student/tags}.
 *
 * <p>Used by KURSADMINs and SYSTEMADMINs to create a new tag.
 * Names are unique case-insensitively.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 *
 * @param name display name of the new tag — must not be blank
 */
public record CreateTagRequestDTO(String name) { }
