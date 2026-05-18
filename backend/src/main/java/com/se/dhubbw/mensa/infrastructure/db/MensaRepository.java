package com.se.dhubbw.mensa.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Mensa} entities.
 */
@Repository
public interface MensaRepository extends JpaRepository<Mensa, UUID> {

    /**
     * Find all meals for a specific date.
     *
     * @param datum the date to query
     * @return list of meals on that date
     */
    List<Mensa> findByDatum(LocalDate datum);

    /**
     * Delete all meals for a specific date.
     *
     * @param datum the date whose meals should be deleted
     */
    @Modifying
    @Query("DELETE FROM Mensa m WHERE m.datum = :datum")
    void deleteByDatum(@Param("datum") LocalDate datum);

    /**
     * Delete all meals whose date is strictly before the given cutoff.
     *
     * @param cutoff meals before this date are removed
     */
    @Modifying
    @Query("DELETE FROM Mensa m WHERE m.datum < :cutoff")
    void deleteBeforeDate(@Param("cutoff") LocalDate cutoff);
}
