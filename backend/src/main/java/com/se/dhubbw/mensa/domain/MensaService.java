package com.se.dhubbw.mensa.domain;

import com.se.dhubbw.mensa.infrastructure.Mensa;

import java.time.LocalDate;
import java.util.List;

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
}
