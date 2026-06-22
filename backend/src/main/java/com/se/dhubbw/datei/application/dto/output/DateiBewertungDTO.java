package com.se.dhubbw.datei.application.dto.output;

/**
 * Aggregated rating data for a single file plus the caller's own
 * rating, returned by {@code GET /student/dateien/{dateiId}/bewertung}.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation)
 *
 * @param durchschnitt Average rating across all submitted votes,
 *     or {@code 0.0} when no rating exists yet.
 * @param anzahl       Total number of votes that contributed to the
 *     average.
 * @param eigeneSterne The caller's own rating (1..5), or {@code 0}
 *     if the caller has not yet rated this file.
 */
public record DateiBewertungDTO(double durchschnitt,
                                int anzahl,
                                int eigeneSterne) { }
