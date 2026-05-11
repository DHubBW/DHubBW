package com.se.dhubbw.kurs.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO with aggregated statistics for a course administrator.
 *
 * <p>Carries the three key figures a KURSADMIN sees on the
 * members overview: total enrolled members, total files in
 * the course, and open join requests pending approval.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KursAdminStatsDTO {

    /**
     * Total number of approved course members.
     */
    private int anzahlMitglieder;

    /**
     * Total number of files belonging to the course.
     */
    private int anzahlDateien;

    /**
     * Number of open (pending) course join requests.
     */
    private int anzahlAnfragen;
}
