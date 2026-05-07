package com.se.dhubbw.kurs.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data repository for {@link Kurs} entities.
 */
@Repository
public interface KursRepository extends JpaRepository<Kurs, UUID> {

    /**
     * Check whether a course with the given name and cohort
     * year already exists.
     *
     * @param kursName the course name to check (e.g. "TINF24B2")
     * @param jahrgang the cohort year to check (e.g. "2024")
     * @return {@code true} if a matching course exists
     */
    boolean existsByKursNameAndJahrgang(String kursName, String jahrgang);
}
