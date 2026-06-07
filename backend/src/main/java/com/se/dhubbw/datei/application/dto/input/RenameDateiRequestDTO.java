package com.se.dhubbw.datei.application.dto.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for renaming a file.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request payload)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RenameDateiRequestDTO {

    /**
     * New display name for the file.
     */
    private String newName;
}
