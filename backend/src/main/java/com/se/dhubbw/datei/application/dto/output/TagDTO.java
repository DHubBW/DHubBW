package com.se.dhubbw.datei.application.dto.output;

import java.util.UUID;

/**
 * Data transfer object representing a tag.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 *
 * @param tagID Unique tag identifier.
 * @param name  Display name of the tag.
 */
public record TagDTO(UUID tagID, String name) { }
