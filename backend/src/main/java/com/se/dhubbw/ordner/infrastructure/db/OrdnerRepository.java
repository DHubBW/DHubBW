package com.se.dhubbw.ordner.infrastructure.db;

import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link Ordner} entities.
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
