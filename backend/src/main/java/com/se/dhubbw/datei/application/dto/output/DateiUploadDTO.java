package com.se.dhubbw.datei.application.dto.output;

import java.util.UUID;

/**
 * Data transfer object for the upload response.
 * Returns the UUID of the newly created file to the client.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 * @param dateiID The ID of the uploaded datei.
 */
public record DateiUploadDTO(UUID dateiID) {
}
