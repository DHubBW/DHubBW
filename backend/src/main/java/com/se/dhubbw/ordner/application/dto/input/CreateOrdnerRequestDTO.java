package com.se.dhubbw.ordner.application.dto.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.se.dhubbw.ordner.domain.OrdnerFarbe;

import java.util.UUID;

/**
 * Request body for creating a new folder.
 *
 * @param ueberOrdnerId UUID of the parent folder (null for root-level)
 * @param name          display name for the new folder
 * @param isPublic      whether the folder is publicly visible
 * @param farbe         color label for the folder
 * @architecture.component Ordnerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 */
public record CreateOrdnerRequestDTO(UUID ueberOrdnerId, String name,
                                     @JsonProperty("isPublic") boolean isPublic,
                                     OrdnerFarbe farbe) { }
