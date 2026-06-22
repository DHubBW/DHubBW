package com.se.dhubbw.shared.infrastructure.backup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for database access to {@link Backup} entities.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data access layer)
 * @architecture.category T-Software (Spring Data JPA)
 */
@Repository
public interface BackupRepository extends JpaRepository<Backup, UUID> {

    /**
     * Retrieve all backups, newest first.
     *
     * @return all backups, sorted by creation timestamp descending
     */
    @Query("SELECT b FROM Backup b ORDER BY b.erstelltAm DESC")
    List<Backup> findAllOrderByErstelltAmDesc();

    /**
     * Retrieve the most recent backup.
     *
     * @return the newest backup, if any
     */
    @Query("SELECT b FROM Backup b ORDER BY b.erstelltAm DESC LIMIT 1")
    Optional<Backup> findLatest();

    /**
     * Delete every backup whose {@code erstellt_am} is strictly
     * older than the given cut-off. Used by the retention sweep so
     * backups older than the configured window vanish without
     * manual intervention.
     *
     * @param cutoff the cut-off timestamp; rows with
     *     {@code erstelltAm < cutoff} are removed
     * @return the number of deleted rows
     */
    @Modifying
    @Query("DELETE FROM Backup b WHERE b.erstelltAm < :cutoff")
    int deleteByErstelltAmBefore(
        @Param("cutoff") LocalDateTime cutoff);
}
