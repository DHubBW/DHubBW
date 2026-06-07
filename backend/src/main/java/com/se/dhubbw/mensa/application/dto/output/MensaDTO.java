package com.se.dhubbw.mensa.application.dto.output;

import com.se.dhubbw.mensa.infrastructure.util.Ernaehrung;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data transfer object for representing a mensa meal in API responses.
 * Contains all relevant meal information for the frontend display.
 *
 * @architecture.component Mensaverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 * @param gerichtID The id of the meal.
 * @param datum The date when the meal is served.
 * @param essensbezeichnung The description of the meal.
 * @param allergene Potential allergens of the meal.
 * @param preis The price of the meal.
 * @param ernaehrung The type of the meal (veggie, vegan or carnivore)
 * @param linie The serving line or category (e.g. "Wahlessen 1")
 */
public record MensaDTO(UUID gerichtID,
                       LocalDate datum,
                       String essensbezeichnung,
                       String allergene,
                       double preis,
                       Ernaehrung ernaehrung,
                       String linie) {
}
