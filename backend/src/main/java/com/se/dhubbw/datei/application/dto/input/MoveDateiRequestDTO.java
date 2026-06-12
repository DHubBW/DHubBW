package com.se.dhubbw.datei.application.dto.input;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for {@code PATCH /student/dateien/{dateiId}/move}.
 *
 * <p>Carries the UUID of the new parent folder. Authorisation is
 * checked against both the source folder (modify rights) and the
 * target folder (upload rights), and a 409 is raised when a file
 * of the same name already exists in the target.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (request representation)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MoveDateiRequestDTO {

    /**
     * UUID of the target folder. Must not be {@code null}; the
     * synthetic "Alle Dateien" root has no folder UUID and is
     * therefore not a valid move target.
     */
    @NotNull
    private UUID targetOrdnerId;
}
