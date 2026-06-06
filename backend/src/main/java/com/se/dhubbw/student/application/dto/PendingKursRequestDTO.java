package com.se.dhubbw.student.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO representing an open (pending) course request of a student.
 *
 * <p>Covers both course join requests ({@code BEITRITT}) and
 * course creation requests ({@code ERSTELLUNG}). The field
 * {@code kursName} is {@code null} for creation requests where
 * the requested name is not yet confirmed.</p>
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
