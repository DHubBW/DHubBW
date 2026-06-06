package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.shared.infrastructure.helper.StudentLookupHelper;
import com.se.dhubbw.student.infrastructure.db.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Shared helper component for student and course resolution in the
 * course module.
 * Centralises recurring lookup patterns across course service
 * implementations.
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Component, module-internal
 *     helper class)
 */
@Component
@RequiredArgsConstructor
public class KursStudentHelper {

    /**
     * Shared helper for student lookups by email or UUID.
     */
    private final StudentLookupHelper studentLookupHelper;

    /**
     * Repository for enrollment lookups.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Find a student by their DHBW email address.
     *
     * @param dhbwMail the DHBW email address to search for
     * @return the matching student
     * @throws NoSuchElementException if no student is found
     */
    public Student findStudentByEmail(final String dhbwMail) {
        return studentLookupHelper.findStudentByEmail(dhbwMail);
    }

    /**
     * Find a student by their UUID.
     *
     * @param studentId the student identifier
     * @return the matching student
     * @throws NoSuchElementException if no student is found
     */
    public Student findStudentById(final UUID studentId) {
        return studentLookupHelper.findStudentById(studentId);
    }

    /**
     * Resolve the approved course enrollment of the given student.
     *
     * @param student the enrolled student
     * @return the course the student is enrolled in
     * @throws NoSuchElementException if no approved enrollment exists
     */
    public Kurs findKursForStudent(final Student student) {
        return kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student)
            .orElseThrow(() -> new NoSuchElementException(
                "No course assignment for: "
                    + student.getDhbwMail()))
            .getKurs();
    }

    /**
     * Resolve the course managed by the admin with the given email.
     *
     * <p>Convenience method combining
     * {@link #findStudentByEmail(String)} and
     * {@link #findKursForStudent(Student)}.</p>
     *
     * @param dhbwMail the admin's DHBW email address
     * @return the admin's course
     * @throws NoSuchElementException if student or enrollment not found
     */
    public Kurs findKursForAdminByEmail(final String dhbwMail) {
        return findKursForStudent(findStudentByEmail(dhbwMail));
    }
}
