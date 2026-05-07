package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service for course-admin enrollment request management.
 *
 * <p>Provides operations for a course administrator to list,
 * approve, or decline pending join requests for their course.
 * The admin's course is determined by their own approved
 * {@link KursBeitrittsAnfrage}.</p>
 */
@Service
@RequiredArgsConstructor
public class KursAdminService {

    /**
     * Repository for student lookup by DHBW mail.
     */
    private final StudentRepository studentRepository;

    /**
     * Repository for enrollment request operations.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Retrieve all pending join requests for the course
     * administered by the given KURSADMIN.
     *
     * @param dhbwMail the DHBW mail address of the course admin
     * @return list of pending requests, may be empty
     * @throws NoSuchElementException if the admin student or their
     *     course assignment is not found
     */
    @Transactional(readOnly = true)
    public List<KursBeitrittsAnfrage> retrieveKursbeitrittsanfragen(
            final String dhbwMail) {
        Kurs kurs = getKursForAdmin(dhbwMail);
        return kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedFalse(kurs);
    }

    /**
     * Approve a pending join request, assigning the student to
     * the admin's course.
     *
     * <p>The student must not already be enrolled in any course.
     * A student may only belong to one course at a time.</p>
     *
     * @param anfrageId the ID of the request to approve
     * @param dhbwMail the DHBW mail address of the course admin
     * @throws NoSuchElementException if the request or the admin
     *     is not found
     * @throws IllegalArgumentException if the request does not
     *     belong to the admin's course
     * @throws IllegalStateException if the student is already
     *     enrolled in another course
     */
    @Transactional
    public void approveKursbeitrittsanfrage(
            final UUID anfrageId,
            final String dhbwMail) {
        Kurs kurs = getKursForAdmin(dhbwMail);
        KursBeitrittsAnfrage anfrage = findAnfrage(anfrageId);
        verifyBelongsToCourse(anfrage, kurs);
        verifyStudentNotEnrolled(anfrage);
        anfrage.setApproved(true);
        kursBeitrittsAnfrageRepository.save(anfrage);
    }

    /**
     * Decline and permanently delete a pending join request.
     *
     * @param anfrageId the ID of the request to decline
     * @param dhbwMail the DHBW mail address of the course admin
     * @throws NoSuchElementException if the request or the admin
     *     is not found
     * @throws IllegalArgumentException if the request does not
     *     belong to the admin's course
     */
    @Transactional
    public void declineKursbeitrittsanfrage(
            final UUID anfrageId,
            final String dhbwMail) {
        Kurs kurs = getKursForAdmin(dhbwMail);
        KursBeitrittsAnfrage anfrage = findAnfrage(anfrageId);
        verifyBelongsToCourse(anfrage, kurs);
        kursBeitrittsAnfrageRepository.delete(anfrage);
    }

    /**
     * Determine the course managed by the given admin via their
     * approved enrollment request.
     *
     * @param dhbwMail the admin's DHBW mail address
     * @return the admin's course
     * @throws NoSuchElementException if the student or their
     *     course assignment is not found
     */
    private Kurs getKursForAdmin(final String dhbwMail) {
        Student student = studentRepository
            .findByDhbwMail(dhbwMail)
            .orElseThrow(() -> new NoSuchElementException(
                "Student not found: " + dhbwMail));
        return kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student)
            .orElseThrow(() -> new NoSuchElementException(
                "No course assignment for: " + dhbwMail))
            .getKurs();
    }

    /**
     * Find a join request by its identifier.
     *
     * @param anfrageId the request ID
     * @return the matching request
     * @throws NoSuchElementException if no request with that ID exists
     */
    private KursBeitrittsAnfrage findAnfrage(final UUID anfrageId) {
        return kursBeitrittsAnfrageRepository
            .findById(anfrageId)
            .orElseThrow(() -> new NoSuchElementException(
                "Anfrage not found: " + anfrageId));
    }

    /**
     * Assert that a request belongs to the given course.
     *
     * @param anfrage the request to verify
     * @param kurs the expected course
     * @throws IllegalArgumentException if the request targets a
     *     different course
     */
    private void verifyBelongsToCourse(
            final KursBeitrittsAnfrage anfrage,
            final Kurs kurs) {
        if (!anfrage.getKurs().getKursID()
                .equals(kurs.getKursID())) {
            throw new IllegalArgumentException(
                "Anfrage does not belong to admin course");
        }
    }

    /**
     * Assert that the requesting student is not already enrolled
     * in any course.
     *
     * @param anfrage the request whose student is to be checked
     * @throws IllegalStateException if the student already has an
     *     approved enrollment
     */
    private void verifyStudentNotEnrolled(
            final KursBeitrittsAnfrage anfrage) {
        if (kursBeitrittsAnfrageRepository
                .existsByStudentAndIsApprovedTrue(anfrage.getStudent())) {
            throw new IllegalStateException(
                "Student is already enrolled in a course");
        }
    }
}
