package com.se.dhubbw.ordner.application.dto.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for renaming a folder.
 *
 * @architecture.component Ordnerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RenameOrdnerRequestDTO {

    /**
     * New display name for the folder.
     */
    private String newName;
}
