package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursRepository;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service for system-admin course creation request management.
 *
 * <p>Provides operations for a system administrator to list,
 * approve, or decline pending course creation requests.
 * On approval, a new course is created and the requesting
 * student is promoted to the {@code KURSADMIN} role with an
 * approved course membership.</p>
 */
@Service
@RequiredArgsConstructor
public class KursSystemAdminService {

    /**
     * Repository for course creation request operations.
     */
    private final KursErstellungsAnfrageRepository
        erstellungsAnfrageRepository;

    /**
     * Repository for course enrollment request operations.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Repository for course operations.
     */
    private final KursRepository kursRepository;

    /**
     * Repository for student operations.
     */
    private final StudentRepository studentRepository;

    /**
     * Retrieve all pending (not yet approved) course creation
     * requests.
     *
     * @return list of pending creation requests; may be empty
     */
    @Transactional(readOnly = true)
    public List<KursErstellungsAnfrage>
            retrieveKursErstellungsanfragen() {
        return erstellungsAnfrageRepository.findByIsApprovedFalse();
    }

    /**
     * Approve a pending course creation request.
     *
     * <p>Creates the new course, promotes the requesting student
     * to {@code KURSADMIN}, and logs course membership via an
     * approved {@link KursBeitrittsAnfrage}.</p>
     *
     * @param anfrageId the ID of the request to approve
     * @throws NoSuchElementException if the request is not found
     * @throws IllegalStateException if the requesting student is
     *     already enrolled in a course
     * @throws IllegalArgumentException if a course with the same
     *     name and cohort year already exists
     */
    @Transactional
    public void approveKursErstellungsanfrage(
            final UUID anfrageId) {
        KursErstellungsAnfrage anfrage = findAnfrageOrThrow(anfrageId);
        Student student = anfrage.getStudent();

        if (kursBeitrittsAnfrageRepository
                .existsByStudentAndIsApprovedTrue(student)) {
            throw new IllegalStateException(
                "Student is already enrolled in a course");
        }

        if (kursRepository.existsByKursNameAndJahrgang(
                anfrage.getKursName(), anfrage.getJahrgang())) {
            throw new IllegalArgumentException(
                "Course already exists: "
                    + anfrage.getKursName()
                    + "/" + anfrage.getJahrgang());
        }

        Kurs neuerKurs = new Kurs();
        neuerKurs.setKursName(anfrage.getKursName());
        neuerKurs.setJahrgang(anfrage.getJahrgang());
        kursRepository.save(neuerKurs);

        student.setStudentType(StudentType.KURSADMIN);
        studentRepository.save(student);

        KursBeitrittsAnfrage beitritt = new KursBeitrittsAnfrage();
        beitritt.setKurs(neuerKurs);
        beitritt.setStudent(student);
        beitritt.setAnfrageDatum(LocalDate.now());
        beitritt.setApproved(true);
        kursBeitrittsAnfrageRepository.save(beitritt);

        anfrage.setApproved(true);
        erstellungsAnfrageRepository.save(anfrage);
    }

    /**
     * Decline and permanently delete a pending course creation
     * request.
     *
     * @param anfrageId the ID of the request to decline
     * @throws NoSuchElementException if the request is not found
     */
    @Transactional
    public void declineKursErstellungsanfrage(
            final UUID anfrageId) {
        KursErstellungsAnfrage anfrage = findAnfrageOrThrow(anfrageId);
        erstellungsAnfrageRepository.delete(anfrage);
    }

    /**
     * Find a creation request by its identifier.
     *
     * @param anfrageId the request ID
     * @return the matching request
     * @throws NoSuchElementException if no request with that ID
     *     exists
     */
    private KursErstellungsAnfrage findAnfrageOrThrow(
            final UUID anfrageId) {
        return erstellungsAnfrageRepository.findById(anfrageId)
            .orElseThrow(() -> new NoSuchElementException(
                "Anfrage not found: " + anfrageId));
    }
}
