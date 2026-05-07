package com.se.dhubbw.kurs.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data repository for {@link KursErstellungsAnfrage} entities.
 */
@Repository
public interface KursErstellungsAnfrageRepository extends
    JpaRepository<KursErstellungsAnfrage, UUID> {

    /**
     * Check whether the student already has an open (pending)
     * course creation request that has not yet been approved.
     *
     * @param student the student to check
     * @return {@code true} if a pending creation request exists
     */
    boolean existsByStudentAndIsApprovedFalse(Student student);
}
