package com.se.dhubbw.kurs.application.dto.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data transfer object with platform-wide statistics for a system admin.
 * Contains key figures for courses, users, files, storage, and open requests.
 *
 * <p>Carries the key figures a SYSTEMADMIN sees on the
 * overview dashboard: total courses, total users, total files,
 * used and available storage, and pending course creation
 * requests.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
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
