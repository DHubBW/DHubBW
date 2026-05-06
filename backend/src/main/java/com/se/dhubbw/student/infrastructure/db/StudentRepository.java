package com.se.dhubbw.student.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link Student} entities.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    /**
     * Find a student by their DHBW email address.
     *
     * @param dhbwMail the DHBW email address to search for
     * @return the matching student wrapped in an Optional,
     *     empty if not found
     */
    Optional<Student> findByDhbwMail(String dhbwMail);
}
