package com.se.dhubbw.kurs.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data repository for {@link KursErstellungsAnfrage} entities.
 */
@Repository
public interface KursErstellungsAnfrageRepository extends
    JpaRepository<KursErstellungsAnfrage, UUID> {
}
