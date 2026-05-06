package com.se.dhubbw.dashboard.domain;

import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.mensa.infrastructure.Mensa;

import java.util.List;
import java.util.Optional;

/**
 * Application service interface for aggregating dashboard data.
 *
 * <p>Defines the operations required by the dashboard to supply
 * today's mensa meals and the latest files visible to the
 * authenticated user.</p>
 */
public interface DashboardService {

    /**
     * Retrieve mensa meals available today.
     *
     * @return list of today's meals, may be empty
     */
    List<Mensa> getMealsForToday();

    /**
     * Retrieve the latest files across all courses,
     * limited to the dashboard maximum.
     *
     * @return all files, newest first
     */
    List<Datei> getAllLatestDateien();

    /**
     * Retrieve the latest files for the student's enrolled course.
     *
     * <p>Returns {@code Optional.empty()} when the student is unknown
     * or has no approved course enrollment. Returns
     * {@code Optional.of(emptyList)} when enrolled but no files exist.</p>
     *
     * @param dhbwMail the DHBW email address identifying the student
     * @return files for the student's course, or empty Optional if
     *     the student has no approved enrollment
     */
    Optional<List<Datei>> getLatestDateienForStudent(String dhbwMail);
}
