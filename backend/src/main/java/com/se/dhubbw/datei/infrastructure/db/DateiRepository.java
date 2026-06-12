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
 * Repository for database access to {@link Datei} entities.
 * Provides CRUD operations and custom queries for file metadata.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data access layer)
 * @architecture.category T-Software (Spring Data JPA)
 */
@Repository
public interface DateiRepository extends JpaRepository<Datei, UUID> {

    /**
     * Retrieve all files ordered by name ascending.
     *
     * @return all files across all courses, sorted by name
     */
    @Query("SELECT d FROM Datei d ORDER BY d.name ASC")
    List<Datei> findAllOrderByNameAsc();

    /**
     * Retrieve all files ordered by last-modified date descending.
     *
     * @return all files across all courses, newest first
     */
    @Query("SELECT d FROM Datei d "
        + "ORDER BY d.zuletztBearbeitet DESC")
    List<Datei> findAllOrderByZuletztBearbeitetDesc();

    /**
     * Retrieve all files belonging to a course, ordered by name
     * ascending.
     *
     * @param kursId the course identifier to filter by
     * @return list of files for the given course, sorted by name
     */
    @Query("SELECT d FROM Datei d JOIN d.ordner o JOIN o.kurs k "
        + "WHERE k.kursID = :kursId ORDER BY d.name ASC")
    List<Datei> findByKursIdOrderByNameAsc(
        @Param("kursId") UUID kursId);

    /**
     * Retrieve all files belonging to a course, ordered by
     * last-modified date descending.
     *
     * @param kursId the course identifier to filter by
     * @return list of files for the given course, newest first
     */
    @Query("SELECT d FROM Datei d JOIN d.ordner o JOIN o.kurs k "
        + "WHERE k.kursID = :kursId "
        + "ORDER BY d.zuletztBearbeitet DESC")
    List<Datei> findByKursIdOrderByZuletztBearbeitetDesc(
        @Param("kursId") UUID kursId);

    /**
     * Retrieve all files in public root-level folders that do not
     * belong to the given course.
     *
     * <p>Used to include public files from other courses in the
     * file listing for students who are in a course. Folders without
     * an assigned course (SYSTEMADMIN-created platform-wide shares)
     * are included since they are by definition not the caller's own
     * course.</p>
     *
     * <p>The explicit {@code o.kurs IS NULL} disjunction prevents the
     * three-valued-logic trap that {@code o.kurs.kursID <> :kursId}
     * would silently drop NULL-kurs rows.</p>
     *
     * @param kursId the course to exclude (the caller's own course)
     * @return list of files in public root folders of other courses,
     *     including platform-wide (NULL-kurs) public root folders
     */
    @Query("SELECT d FROM Datei d JOIN d.ordner o "
        + "LEFT JOIN o.kurs k "
        + "WHERE o.isPublic = TRUE "
        + "AND o.ueberOrdner IS NULL "
        + "AND (k IS NULL OR k.kursID <> :kursId) "
        + "ORDER BY d.name ASC")
    List<Datei> findInPublicRootFoldersExcludingKurs(
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
     * Retrieve all files in a specific folder, ordered by name
     * ascending.
     *
     * @param ordnerId the folder identifier to filter by
     * @return list of files in the given folder, sorted by name
     */
    @Query("SELECT d FROM Datei d "
        + "WHERE d.ordner.ordnerID = :ordnerId "
        + "ORDER BY d.name ASC")
    List<Datei> findByOrdnerIdOrderByNameAsc(
        @Param("ordnerId") UUID ordnerId);

    /**
     * Set the uploader field to null for all files uploaded by
     * the given student.
     *
     * @param uploader the student whose upload references to clear
     */
    @Modifying
    @Query("UPDATE Datei d SET d.uploader = NULL "
        + "WHERE d.uploader = :uploader")
    void setUploaderToNullByUploader(
        @Param("uploader") Student uploader);

    /**
     * Look up a file in the given folder by name.
     *
     * <p>Used by the upload / rename flows to enforce per-folder
     * file-name uniqueness — a folder must not contain two files
     * sharing the same display name.</p>
     *
     * @param ordnerId the folder to search in
     * @param name     the exact display name to match
     * @return the matching file, if any
     */
    @Query("SELECT d FROM Datei d "
        + "WHERE d.ordner.ordnerID = :ordnerId "
        + "AND d.name = :name")
    java.util.Optional<Datei> findByOrdnerIdAndName(
        @Param("ordnerId") UUID ordnerId,
        @Param("name") String name);
}
