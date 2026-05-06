package com.se.dhubbw.datei.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data repository for {@link DateiAnlegung} entities.
 */
@Repository
public interface DateiAnlegungRepository extends
    JpaRepository<DateiAnlegung, UUID> {
}
