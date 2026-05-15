package com.se.dhubbw.datei.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link Datei} entities.
 */
@Repository
public interface DateiRepository extends JpaRepository<Datei, UUID> {

    /**
     * Retrieve all files ordered by last modification date descending.
     *
     * @return all files across all courses, newest first
     */
    @Query("SELECT d FROM Datei d ORDER BY d.zuletztBearbeitet DESC")
    List<Datei> findAllOrderByZuletztBearbeitetDesc();

    /**
     * Retrieve all files belonging to a course, ordered by last
     * modification date descending.
     *
     * @param kursId the course identifier to filter by
     * @return list of files for the given course, newest first
     */
    @Query("SELECT d FROM Datei d JOIN d.ordner o JOIN o.kurs k "
        + "WHERE k.kursID = :kursId ORDER BY d.zuletztBearbeitet DESC")
    List<Datei> findByKursIdOrderByZuletztBearbeitetDesc(
        @Param("kursId") UUID kursId);

    /**
     * Compute the sum of all file sizes stored on the platform.
     *
     * @return total file size in megabytes, or 0.0 if no files
     *     exist
     */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0.0) FROM Datei d")
    double findTotalFileSizeInMb();

    /**
     * Set the uploader field to null for all files uploaded by
     * the given student.
     *
     * @param uploader the student whose upload references to clear
     */
    @Modifying
    @Query("UPDATE Datei d SET d.uploader = NULL WHERE d.uploader = :uploader")
    void setUploaderToNullByUploader(@Param("uploader") Student uploader);
}


