package com.se.dhubbw.kurs.application.dto;

/**
 * DTO with aggregated statistics for a course administrator.
 *
 * <p>Carries the three key figures a KURSADMIN sees on the
 * members overview: total enrolled members, total files in
 * the course, and open join requests pending approval.</p>
 * @param anzahlMitglieder The amount of students assigned to the course.
 * @param anzahlDateien The amount of files assigned to the course.
 * @param anzahlAnfragen The amount of join requests for the course.
 */
public record KursAdminStatsDTO(
    int anzahlMitglieder,
    int anzahlDateien,
    int anzahlAnfragen) { }
