package com.se.dhubbw.datei.application.dto;

import java.util.UUID;

/**
 * Data transfer object (DTO) which is sent to the user when uploading a datei.
 * @param dateiID The ID of the uploaded datei.
 */
public record DateiUploadDTO(UUID dateiID) {
}
