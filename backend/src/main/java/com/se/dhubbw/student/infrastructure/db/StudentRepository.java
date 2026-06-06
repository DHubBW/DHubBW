package com.se.dhubbw.student.infrastructure.db;

import com.se.dhubbw.student.infrastructure.util.StudentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for database access to {@link Student} entities.
 * Provides queries by email and role counting.
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Infrastructure Layer (data access layer)
 * @architecture.category T-Software (Spring Data JPA)
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

    /**
     * Count the number of students with the given role.
     *
     * @param studentType the role to count
     * @return number of students with that role
     */
    long countByStudentType(StudentType studentType);
}

