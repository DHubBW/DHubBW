package com.se.dhubbw.ordner.application.dto.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.se.dhubbw.ordner.domain.OrdnerFarbe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request body for creating a new folder.
 *
 * @architecture.component Ordnerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CreateOrdnerRequestDTO {

    /**
     * UUID of the parent folder (null for root-level).
     */
    private UUID ueberOrdnerId;

    /**
     * Display name for the new folder.
     */
    private String name;

    /**
     * Whether the folder is publicly visible.
     */
    @JsonProperty("isPublic")
    private boolean isPublic;

    /**
     * Color label for the folder.
     */
    private OrdnerFarbe farbe;
}
