package com.se.dhubbw.datei.application.dto.input;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for {@code PATCH /student/dateien/{dateiId}/move}.
 *
 * <p>Carries the UUID of the new parent folder. Authorisation is
 * checked against both the source folder (modify rights) and the
 * target folder (upload rights), and a 409 is raised when a file
 * of the same name already exists in the target.</p>
 *
 * @param targetOrdnerId UUID of the target folder. Must not be {@code null}
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request representation)
 */
public record MoveDateiRequestDTO(@NotNull UUID targetOrdnerId) { }
