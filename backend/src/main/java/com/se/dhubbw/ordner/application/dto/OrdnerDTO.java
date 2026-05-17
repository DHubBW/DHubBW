package com.se.dhubbw.ordner.application.dto;

import com.se.dhubbw.ordner.util.OrdnerFarbe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for folder metadata.
 *
 * <p>Carries display-relevant metadata only;
 * file contents are intentionally excluded.</p>
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
