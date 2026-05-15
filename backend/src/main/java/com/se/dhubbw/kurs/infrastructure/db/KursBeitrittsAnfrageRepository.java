package com.se.dhubbw.kurs.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link KursBeitrittsAnfrage} entities.
 */
@Repository
public interface KursBeitrittsAnfrageRepository extends
    JpaRepository<KursBeitrittsAnfrage, UUID> {

    /**
     * Find the approved course enrollment request for the given student.
     *
     * @param student the student whose approved enrollment to look up
     * @return the approved enrollment request, or empty if not found
     */
    Optional<KursBeitrittsAnfrage> findByStudentAndIsApprovedTrue(
        Student student);

    /**
     * Find all pending (not yet approved) enrollment requests
     * for the given course.
     *
     * @param kurs the course whose pending requests to retrieve
     * @return list of pending requests, may be empty
     */
    List<KursBeitrittsAnfrage> findByKursAndIsApprovedFalse(Kurs kurs);

    /**
     * Check whether the student already has an approved course enrollment.
     *
     * @param student the student to check
     * @return {@code true} if an approved enrollment request exists
     */
    boolean existsByStudentAndIsApprovedTrue(Student student);

    /**
     * Check whether the student already has an open join request
     * that has not yet been approved.
     *
     * @param student the student to check
     * @return {@code true} if a pending join request exists
     */
    boolean existsByStudentAndIsApprovedFalse(Student student);

    /**
     * Find all approved course enrollments for the given course.
     *
     * @param kurs the course whose approved members to retrieve
     * @return list of approved enrollment requests (course members)
     */
    List<KursBeitrittsAnfrage> findByKursAndIsApprovedTrue(Kurs kurs);

    /**
     * Find all enrollment requests (approved and pending) for the
     * given course.
     *
     * @param kurs the course whose enrollment records to retrieve
     * @return list of all enrollment requests for the course
     */
    List<KursBeitrittsAnfrage> findByKurs(Kurs kurs);
}

