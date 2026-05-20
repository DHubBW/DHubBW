package com.se.dhubbw.ordner.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link OrdnerAnlegung} entities.
 */
@Repository
public interface OrdnerAnlegungRepository extends
    JpaRepository<OrdnerAnlegung, UUID> {

    /**
     * Find the creation record for the given folder.
     *
     * @param ordner the folder whose creation record to look up
     * @return the matching creation record, or empty if not found
     */
    Optional<OrdnerAnlegung> findByOrdner(Ordner ordner);

    /**
     * Delete all folder creation records for the given student.
     *
     * @param student the student whose creation records to delete
     */
    void deleteByStudent(Student student);
}
