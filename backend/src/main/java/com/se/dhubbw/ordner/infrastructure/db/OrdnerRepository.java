package com.se.dhubbw.ordner.infrastructure.db;

import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for database access to {@link Ordner} entities.
 * Provides CRUD operations and custom queries for folder data.
 *
 * @architecture.component Ordnerverwaltung
 * @architecture.layer Infrastructure Layer (data access layer)
 * @architecture.category T-Software (Spring Data JPA)
 */
@Repository
public interface OrdnerRepository extends JpaRepository<Ordner, UUID> {

    /**
     * Find all folders belonging to the given course.
     *
     * @param kurs the course whose folders to retrieve
     * @return list of folders for the given course, may be empty
     */
    List<Ordner> findByKurs(Kurs kurs);

    /**
     * Find all root-level folders for the given course.
     *
     * @param kurs the course whose root folders to retrieve
     * @return list of root folders, may be empty
     */
    List<Ordner> findByKursAndUeberOrdnerIsNull(Kurs kurs);

    /**
     * Find all root-level public folders not belonging to the given course.
     *
     * <p>Used to show public folders from other courses to students who are
     * enrolled in a different course. Folders without an assigned course
     * (created by SYSTEMADMIN as platform-wide shares) are included as
     * well, since they are by definition not the caller's own course.</p>
     *
     * <p>The explicit {@code o.kurs IS NULL} disjunction is required: a
     * naked {@code o.kurs.kursID <> :kursId} predicate would (a) trigger
     * an implicit inner join on {@code Kurs} that already drops NULL-kurs
     * rows, and (b) evaluate to UNKNOWN — and therefore filter the row
     * out — when the underlying value is NULL (SQL three-valued logic).
     * </p>
     *
     * @param kursId the ID of the course to exclude
     * @return list of root-level public folders from other courses,
     *     including platform-wide (NULL-kurs) public root folders
     */
    @Query("SELECT o FROM Ordner o LEFT JOIN o.kurs k"
        + " WHERE (k IS NULL OR k.kursID <> :kursId)"
        + " AND o.ueberOrdner IS NULL AND o.isPublic = TRUE")
    List<Ordner> findPublicRootFoldersExcludingKurs(
        @Param("kursId") UUID kursId);

    /**
     * Find all root-level folders across all courses.
     *
     * @return list of all root folders, may be empty
     */
    List<Ordner> findByUeberOrdnerIsNull();

    /**
     * Find all direct child folders of the given parent folder.
     *
     * @param ueberOrdner the parent folder
     * @return list of direct child folders, may be empty
     */
    List<Ordner> findChildrenByUeberOrdner(Ordner ueberOrdner);

    /**
     * Check whether the given folder has at least one direct child.
     *
     * <p>Used by the mapping layer to populate
     * {@code OrdnerDTO.hasChildren} so clients can hide the
     * "expand"-chevron for leaf folders without paying the cost of
     * loading the full child list.</p>
     *
     * @param ueberOrdner the parent folder
     * @return {@code true} if at least one child folder references the
     *     given parent
     */
    boolean existsByUeberOrdner(Ordner ueberOrdner);

    /**
     * Look up a direct child of the given parent by name.
     *
     * <p>Used to enforce per-parent folder-name uniqueness in
     * the create / rename flows.</p>
     *
     * @param ueberOrdner the parent folder
     * @param name        the exact display name to match
     * @return the matching child folder, if any
     */
    java.util.Optional<Ordner> findByUeberOrdnerAndName(
        Ordner ueberOrdner, String name);

    /**
     * Look up a top-level folder of the given course by name.
     *
     * <p>Top-level folders have a {@code null} parent reference, so
     * the parent-based uniqueness check is split into this
     * dedicated query.</p>
     *
     * @param kurs the course owning the folder
     * @param name the exact display name to match
     * @return the matching root folder, if any
     */
    java.util.Optional<Ordner> findByKursAndUeberOrdnerIsNullAndName(
        Kurs kurs, String name);

    /**
     * Look up a top-level folder by name when no course is set.
     *
     * <p>SYSTEMADMINs may create top-level folders that are not
     * attached to any course. This query lets the uniqueness
     * check work for that fringe case too.</p>
     *
     * @param name the exact display name to match
     * @return the matching root folder, if any
     */
    java.util.Optional<Ordner>
        findByKursIsNullAndUeberOrdnerIsNullAndName(String name);
}
