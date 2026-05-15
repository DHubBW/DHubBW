package com.se.dhubbw.datei.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link DateiAnlegung} entities.
 */
@Repository
public interface DateiAnlegungRepository extends
    JpaRepository<DateiAnlegung, UUID> {

    /**
     * Find the creation record for the given file.
     *
     * @param datei the file whose creation record to look up
     * @return the matching creation record, or empty if not found
     */
    Optional<DateiAnlegung> findByDatei(Datei datei);
}
