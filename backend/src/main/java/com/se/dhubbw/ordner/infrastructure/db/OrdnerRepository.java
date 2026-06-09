package com.se.dhubbw.ordner.infrastructure.db;

import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * enrolled in a different course.</p>
     *
     * @param kurs the course to exclude
     * @return list of root-level public folders from other courses
     */
    List<Ordner> findByKursNotAndUeberOrdnerIsNullAndIsPublicTrue(Kurs kurs);

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
}
