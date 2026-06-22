package com.se.dhubbw.datei.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for database access to {@link Bewertung} entities.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data access layer)
 * @architecture.category T-Software (Spring Data JPA)
 */
@Repository
public interface BewertungRepository
    extends JpaRepository<Bewertung, UUID> {

    /**
     * Find the rating issued by the given student on the given file.
     *
     * @param dateiId the UUID of the file
     * @param autorId the UUID of the rating student
     * @return the matching rating, if any
     */
    @Query("SELECT b FROM Bewertung b "
        + "WHERE b.datei.dateiID = :dateiId "
        + "AND b.autor.studentID = :autorId")
    Optional<Bewertung> findByDateiAndAutor(
        @Param("dateiId") UUID dateiId,
        @Param("autorId") UUID autorId);

    /**
     * Delete every rating issued by the given student.
     *
     * @param autor the student whose ratings to delete
     */
    void deleteByAutor(Student autor);

    /**
     * Delete every rating attached to the given file.
     *
     * @param datei the file whose ratings to delete
     */
    void deleteByDatei(Datei datei);
}
