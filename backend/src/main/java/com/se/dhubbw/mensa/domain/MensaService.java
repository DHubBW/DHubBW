package com.se.dhubbw.mensa.domain;

import com.se.dhubbw.mensa.infrastructure.db.Mensa;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service interface for mensa meal data management.
 *
 * <p>Defines operations for synchronising meal data from the
 * Studierendenwerk Karlsruhe and querying meals by date.</p>
 */
public interface MensaService {

    /**
     * Fetch the current meal plan and persist it, overwriting any existing
     * records for the same dates. Meals older than four weeks are removed.
     *
     * <p>Groups scraped meals by date and replaces each date's records
     * atomically within a transaction.
     */
    void sync();

    /**
     * Trigger a mensa sync automatically on schedule.
     */
    void scheduledSync();

    /**
     * Retrieve all stored meals for the given date.
     *
     * @param datum the date to query
     * @return list of meals, may be empty
     */
    List<Mensa> getMealsForDay(LocalDate datum);

    /**
     * Retrieve meals for the current calendar week.
     *
     * <p>Returns a map where keys are weekday abbreviations
     * ("Mo", "Di", "Mi", "Do", "Fr") and values are lists of meals.</p>
     *
     * @return map of weekdays to meal lists
     */
    Map<String, List<Mensa>> getMealsForCurrentWeek();

    /**
     * Retrieve meals for the next calendar week.
     *
     * <p>Returns a map where keys are weekday abbreviations
     * ("Mo", "Di", "Mi", "Do", "Fr") and values are lists of meals.</p>
     *
     * @return map of weekdays to meal lists
     */
    Map<String, List<Mensa>> getMealsForNextWeek();
}
