package com.se.dhubbw.student.application.dto.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PendingKursRequestDTO {

    /**
     * Type of the pending request.
     * Either {@code "BEITRITT"} or {@code "ERSTELLUNG"}.
     */
    private String type;

    /**
     * Name of the target course.
     * May be {@code null} for creation requests.
     */
    private String kursName;

    /**
     * Date the request was submitted.
     */
    private LocalDate anfrageDatum;
}
