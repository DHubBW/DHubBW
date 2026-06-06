package com.se.dhubbw.kurs.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for database access to {@link Kurs} entities.
 * Provides CRUD operations and duplicate checking for courses.
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Infrastructure Layer (data access layer)
 * @architecture.category T-Software (Spring Data JPA)
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
