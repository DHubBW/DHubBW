package com.se.dhubbw.kurs.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Find all pending (not yet approved) course creation requests.
     *
     * @return list of pending creation requests, may be empty
     */
    List<KursErstellungsAnfrage> findByIsApprovedFalse();

    /**
     * Count all pending (not yet approved) course creation
     * requests.
     *
     * @return number of pending creation requests
     */
    long countByIsApprovedFalse();

    /**
     * Delete all course creation requests for the given student.
     *
     * @param student the student whose creation requests to delete
     */
    void deleteByStudent(Student student);

    /**
     * Find all course creation requests that target the given course
     * name and cohort year.
     *
     * <p>{@link KursErstellungsAnfrage} has no foreign key to
     * {@link Kurs} (the course does not yet exist when the request
     * is filed), so requests are matched by the name/year tuple
     * that uniquely identifies a course in the system.</p>
     *
     * @param kursName the desired course name
     * @param jahrgang the desired cohort year
     * @return list of matching requests, may be empty
     */
    List<KursErstellungsAnfrage> findByKursNameAndJahrgang(
        String kursName, String jahrgang);
}

