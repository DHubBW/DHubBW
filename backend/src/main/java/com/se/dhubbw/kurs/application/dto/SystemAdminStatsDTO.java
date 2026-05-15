package com.se.dhubbw.kurs.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object with platform-wide statistics for a
 * system administrator.
 *
 * <p>Carries the key figures a SYSTEMADMIN sees on the
 * overview dashboard: total courses, total users, total files,
 * used and available storage, and pending course creation
 * requests.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SystemAdminStatsDTO {

    /**
     * Total number of courses on the platform.
     */
    private long anzahlKurse;

    /**
     * Total number of registered users.
     */
    private long anzahlNutzer;

    /**
     * Total number of files stored on the platform.
     */
    private long anzahlDateien;

    /**
     * Total storage used by all files, in megabytes.
     */
    private double megabyteBelegt;

    /**
     * Remaining storage capacity, in megabytes.
     */
    private double megabyteVerfuegbar;

    /**
     * Number of pending (unapproved) course creation requests.
     */
    private long anzahlKurserstellungsanfragen;
}
