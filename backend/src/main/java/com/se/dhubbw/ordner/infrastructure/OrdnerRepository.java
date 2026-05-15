package com.se.dhubbw.ordner.infrastructure;

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
}

