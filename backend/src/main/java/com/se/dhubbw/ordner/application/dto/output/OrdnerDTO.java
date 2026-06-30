package com.se.dhubbw.ordner.application.dto.output;

import com.se.dhubbw.ordner.domain.OrdnerFarbe;

import java.util.UUID;

/**
 * Data transfer object for representing a folder in API responses.
 * Contains metadata such as name, level, color, and visibility.
 *
 * <p>Carries display-relevant metadata only;
 * file contents are intentionally excluded.</p>
 *
 * @param ordnerID     unique folder identifier
 * @param name         folder name
 * @param level        nesting level (0 = root folder)
 * @param farbe        folder color
 * @param isPublic     whether the folder is publicly visible
 * @param ueberOrdnerID parent folder identifier, or null for root folders
 * @param kursID       identifier of the course this folder belongs to
 * @param hasChildren  whether the folder has at least one direct child folder
 * @architecture.component Ordnerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
public record OrdnerDTO(UUID ordnerID, String name, int level,
                        OrdnerFarbe farbe, boolean isPublic,
                        UUID ueberOrdnerID, UUID kursID,
                        boolean hasChildren) { }
