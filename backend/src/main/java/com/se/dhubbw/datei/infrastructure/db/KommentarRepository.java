package com.se.dhubbw.datei.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for database access to {@link Kommentar} entities.
 * Provides CRUD operations and per-file lookup queries.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data access layer)
 * @architecture.category T-Software (Spring Data JPA)
 */
@Repository
public interface KommentarRepository
    extends JpaRepository<Kommentar, UUID> {

    /**
     * Retrieve all comments belonging to the given file, ordered by
     * creation date ascending (oldest first).
     *
     * @param dateiId the UUID of the file whose comments to load
     * @return list of comments for the given file
     */
    @Query("SELECT k FROM Kommentar k "
        + "WHERE k.datei.dateiID = :dateiId "
        + "ORDER BY k.erstelltAm ASC")
    List<Kommentar> findByDateiIdOrderByErstelltAmAsc(
        @Param("dateiId") UUID dateiId);

    /**
     * Delete all comments authored by the given student.
     *
     * <p>Used when a student is removed from the platform so no
     * orphan author references remain.</p>
     *
     * @param autor the student whose comments to delete
     */
    void deleteByAutor(Student autor);

    /**
     * Delete all comments attached to the given file.
     *
     * <p>Invoked from {@code deleteDatei} so removing a file also
     * removes its comment thread.</p>
     *
     * @param datei the file whose comments to delete
     */
    void deleteByDatei(Datei datei);
}
