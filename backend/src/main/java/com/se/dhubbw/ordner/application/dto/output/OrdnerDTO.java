package com.se.dhubbw.ordner.application.dto.output;

import com.se.dhubbw.ordner.domain.OrdnerFarbe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data transfer object for representing a folder in API responses.
 * Contains metadata such as name, level, color, and visibility.
 *
 * <p>Carries display-relevant metadata only;
 * file contents are intentionally excluded.</p>
 *
 * @architecture.component Ordnerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OrdnerDTO {

    /**
     * Unique folder identifier.
     */
    private UUID ordnerID;

    /**
     * Folder name.
     */
    private String name;

    /**
     * Nesting level (0 = root folder).
     */
    private int level;

    /**
     * Folder color.
     */
    private OrdnerFarbe farbe;

    /**
     * Whether the folder is publicly visible.
     */
    private boolean isPublic;

    /**
     * Parent folder identifier, or null for root folders.
     */
    private UUID ueberOrdnerID;

    /**
     * Identifier of the course this folder belongs to.
     */
    private UUID kursID;
}
