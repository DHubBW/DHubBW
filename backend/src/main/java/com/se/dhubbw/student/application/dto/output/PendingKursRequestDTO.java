package com.se.dhubbw.student.application.dto.output;

import java.time.LocalDate;

/**
 * Data transfer object for a pending course request of a student.
 * Contains request type (BEITRITT/ERSTELLUNG), course name, and
 * submission date.
 *
 * <p>Covers both course join requests ({@code BEITRITT}) and
 * course creation requests ({@code ERSTELLUNG}). The field
 * {@code kursName} is {@code null} for creation requests where
 * the requested name is not yet confirmed.</p>
 *
 * @param type        type of the pending request
 * ({@code "BEITRITT"} or {@code "ERSTELLUNG"})
 * @param kursName    name of the target course (may be {@code null}
 * for creation requests)
 * @param anfrageDatum date the request was submitted
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
public record PendingKursRequestDTO(
    String type, String kursName, LocalDate anfrageDatum) { }
