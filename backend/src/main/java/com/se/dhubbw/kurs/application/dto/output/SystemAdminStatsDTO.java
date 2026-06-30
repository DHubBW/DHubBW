package com.se.dhubbw.kurs.application.dto.output;

/**
 * Data transfer object with platform-wide statistics for a system admin.
 * Contains key figures for courses, users, files, storage, and open requests.
 *
 * <p>Carries the key figures a SYSTEMADMIN sees on the
 * overview dashboard: total courses, total users, total files,
 * used and available storage, and pending course creation
 * requests.</p>
 *
 * @param anzahlKurse                  total number of courses on the platform
 * @param anzahlNutzer                 total number of registered users
 * @param anzahlDateien  total number of files stored on the platform
 * @param megabyteBelegt total storage used by all files, in megabytes
 * @param megabyteVerfuegbar remaining storage capacity, in megabytes
 * @param anzahlKurserstellungsanfragen number of pending (unapproved)
 *     course creation requests
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 */
public record SystemAdminStatsDTO(long anzahlKurse, long anzahlNutzer,
                                  long anzahlDateien,
                                  double megabyteBelegt,
                                  double megabyteVerfuegbar,
                                  long anzahlKurserstellungsanfragen) { }
