package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.application.dto.KursAdminStatsDTO;
import com.se.dhubbw.kurs.application.dto.KursmemberDTO;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service for course member and role management.
 *
 * <p>Provides operations for a KURSADMIN to retrieve member
 * statistics, list all members, manage KURSADMIN roles, and
 * remove members from their course.</p>
 *
 * <p>Business rules enforced:</p>
 * <ul>
 *   <li>At least one KURSADMIN must remain in the course at
 *       all times.</li>
 *   <li>SYSTEMADMIN cannot be promoted to KURSADMIN.</li>
 *   <li>KURSADMIN cannot be removed from course
 *       membership.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class KursMitgliederService {

    /**
     * Repository for student lookup and role updates.
     */
    private final StudentRepository studentRepository;

    /**
     * Repository for enrollment and membership queries.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Repository for counting files per course.
     */
    private final DateiRepository dateiRepository;

    /**
     * Color-coded logger for error reporting.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(KursMitgliederService.class);

    /**
     * Retrieve aggregated statistics for the admin's course.
     *
     * @param dhbwMail the DHBW mail address of the course admin
     * @return a {@link KursAdminStatsDTO} with member count,
     *     file count, and open request count
     * @throws NoSuchElementException if the admin student or
     *     their course assignment is not found
     */
    @Transactional(readOnly = true)
    public KursAdminStatsDTO retrieveKursAdminStats(
            final String dhbwMail) {
        Kurs kurs = getKursForAdmin(dhbwMail);
        int anzahlMitglieder = kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs).size();
        int anzahlDateien = dateiRepository
            .findByKursIdOrderByZuletztBearbeitetDesc(
                kurs.getKursID()).size();
        int anzahlAnfragen = kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedFalse(kurs).size();
        return new KursAdminStatsDTO(
            anzahlMitglieder, anzahlDateien, anzahlAnfragen);
    }

    /**
     * Retrieve all approved members of the admin's course.
     *
     * @param dhbwMail the DHBW mail address of the course admin
     * @return list of {@link KursmemberDTO}; may be empty
     * @throws NoSuchElementException if the admin student or
     *     their course assignment is not found
     */
    @Transactional(readOnly = true)
    public List<KursmemberDTO> retrieveAllCourseMembers(
            final String dhbwMail) {
        Kurs kurs = getKursForAdmin(dhbwMail);
        return kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs).stream()
            .map(enrollment -> new KursmemberDTO(
                enrollment.getStudent().getStudentID(),
                enrollment.getStudent().getName(),
                enrollment.getStudent().getDhbwMail(),
                enrollment.getStudent().getStudentType()))
            .toList();
    }

    /**
     * Give up the calling admin's KURSADMIN role.
     *
     * <p>The admin's student type is set to {@code STUDENT}.
     * This operation requires that at least one other KURSADMIN
     * remains enrolled in the course.</p>
     *
     * @param dhbwMail the DHBW mail address of the course admin
     * @throws NoSuchElementException if the admin student or
     *     their course assignment is not found
     * @throws IllegalStateException if the admin is the only
     *     KURSADMIN in the course
     */
    @Transactional
    public void kursadminNiederlegen(final String dhbwMail) {
        Student admin = findStudentOrThrow(dhbwMail);
        Kurs kurs = getKursForAdmin(admin);
        verifyMinOneOtherKursadmin(kurs, admin);
        admin.setStudentType(StudentType.STUDENT);
        studentRepository.save(admin);
    }

    /**
     * Promote a course member to KURSADMIN.
     *
     * <p>The target student must be a {@code STUDENT} and a
     * member of the admin's course. Promoting a
     * {@code SYSTEMADMIN} is not allowed.</p>
     *
     * @param studentId the ID of the student to promote
     * @param dhbwMail  the DHBW mail address of the course admin
     * @throws NoSuchElementException if the admin or the target
     *     student is not found, or the target is not enrolled
     *     in the admin's course
     * @throws IllegalArgumentException if the target is a
     *     {@code SYSTEMADMIN}
     * @throws IllegalStateException if the target is already
     *     a {@code KURSADMIN}
     */
    @Transactional
    public void addKursadmin(
            final UUID studentId,
            final String dhbwMail) {
        Kurs kurs = getKursForAdmin(dhbwMail);
        Student target = findStudentById(studentId);
        verifyMembership(target, kurs);
        if (target.getStudentType() == StudentType.SYSTEMADMIN) {
            IllegalArgumentException ex = new IllegalArgumentException(
                "Cannot promote SYSTEMADMIN to KURSADMIN");
            colorLogger.colorError(
                "Attempted to promote SYSTEMADMIN {}: {}",
                target.getDhbwMail(), ex.getMessage(), ex);
            throw ex;
        }
        if (target.getStudentType() == StudentType.KURSADMIN) {
            IllegalStateException ex = new IllegalStateException(
                "Student is already KURSADMIN");
            colorLogger.colorError(
                "Student {} is already KURSADMIN: {}",
                target.getDhbwMail(), ex.getMessage(), ex);
            throw ex;
        }
        target.setStudentType(StudentType.KURSADMIN);
        studentRepository.save(target);
    }

    /**
     * Revoke a course member's KURSADMIN role.
     *
     * <p>The target student must be a {@code KURSADMIN} and a
     * member of the admin's course. At least one other KURSADMIN
     * must remain enrolled after the operation.</p>
     *
     * @param studentId the ID of the KURSADMIN to demote
     * @param dhbwMail  the DHBW mail address of the course admin
     * @throws NoSuchElementException if the admin or the target
     *     student is not found, or the target is not enrolled
     *     in the admin's course
     * @throws IllegalArgumentException if the target is not a
     *     {@code KURSADMIN}
     * @throws IllegalStateException if the target is the only
     *     KURSADMIN in the course
     */
    @Transactional
    public void removeKursadmin(
            final UUID studentId,
            final String dhbwMail) {
        Kurs kurs = getKursForAdmin(dhbwMail);
        Student target = findStudentById(studentId);
        verifyMembership(target, kurs);
        if (target.getStudentType() != StudentType.KURSADMIN) {
            IllegalArgumentException ex = new IllegalArgumentException(
                "Student is not a KURSADMIN");
            colorLogger.colorError(
                "Student {} is not a KURSADMIN: {}",
                target.getDhbwMail(), ex.getMessage(), ex);
            throw ex;
        }
        verifyMinOneOtherKursadmin(kurs, target);
        target.setStudentType(StudentType.STUDENT);
        studentRepository.save(target);
    }

    /**
     * Remove a student from the course.
     *
     * <p>The target student must be a {@code STUDENT} and an
     * approved member of the admin's course. KURSADMIN and
     * SYSTEMADMIN members cannot be removed.</p>
     *
     * @param studentId the ID of the student to remove
     * @param dhbwMail  the DHBW mail address of the course admin
     * @throws NoSuchElementException if the admin or the target
     *     student is not found, or the target is not enrolled
     *     in the admin's course
     * @throws IllegalStateException if the target is a
     *     {@code KURSADMIN} or {@code SYSTEMADMIN}
     */
    @Transactional
    public void removeKursMember(
            final UUID studentId,
            final String dhbwMail) {
        Kurs kurs = getKursForAdmin(dhbwMail);
        Student target = findStudentById(studentId);
        KursBeitrittsAnfrage membership =
            findMembership(target, kurs);
        if (target.getStudentType() != StudentType.STUDENT) {
            IllegalStateException ex = new IllegalStateException(
                "Cannot remove KURSADMIN or SYSTEMADMIN"
                    + " from course");
            colorLogger.colorError(
                "Attempted to remove elevated user {}: {}",
                target.getDhbwMail(), ex.getMessage(), ex);
            throw ex;
        }
        kursBeitrittsAnfrageRepository.delete(membership);
    }

    /**
     * Determine the course managed by the given admin.
     *
     * @param dhbwMail the admin's DHBW mail address
     * @return the admin's course
     * @throws NoSuchElementException if the student or their
     *     approved enrollment is not found
     */
    private Kurs getKursForAdmin(final String dhbwMail) {
        return getKursForAdmin(findStudentOrThrow(dhbwMail));
    }

    /**
     * Determine the course managed by the given admin.
     *
     * @param admin the admin student
     * @return the admin's course
     * @throws NoSuchElementException if no approved enrollment
     *     is found for the given student
     */
    private Kurs getKursForAdmin(final Student admin) {
        return kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(admin)
            .orElseThrow(() -> new NoSuchElementException(
                "No course assignment for: "
                    + admin.getDhbwMail()))
            .getKurs();
    }

    /**
     * Find a student by DHBW mail address.
     *
     * @param dhbwMail the DHBW mail address to search for
     * @return the matching student
     * @throws NoSuchElementException if no student is found
     */
    private Student findStudentOrThrow(final String dhbwMail) {
        return studentRepository.findByDhbwMail(dhbwMail)
            .orElseThrow(() -> new NoSuchElementException(
                "Student not found: " + dhbwMail));
    }

    /**
     * Find a student by their UUID.
     *
     * @param studentId the student identifier
     * @return the matching student
     * @throws NoSuchElementException if no student is found
     */
    private Student findStudentById(final UUID studentId) {
        return studentRepository.findById(studentId)
            .orElseThrow(() -> new NoSuchElementException(
                "Student not found: " + studentId));
    }

    /**
     * Find and return the approved enrollment of the given
     * student in the given course.
     *
     * @param student the student whose enrollment to find
     * @param kurs    the course the student must be enrolled in
     * @return the matching approved enrollment request
     * @throws NoSuchElementException if the student has no
     *     approved enrollment or is enrolled in a different
     *     course
     */
    private KursBeitrittsAnfrage findMembership(
            final Student student,
            final Kurs kurs) {
        KursBeitrittsAnfrage membership =
            kursBeitrittsAnfrageRepository
                .findByStudentAndIsApprovedTrue(student)
                .orElseThrow(() -> new NoSuchElementException(
                    "Student is not enrolled in any course"));
        if (!membership.getKurs().getKursID()
                .equals(kurs.getKursID())) {
            throw new NoSuchElementException(
                "Student is not a member of this course");
        }
        return membership;
    }

    /**
     * Assert that the given student is an approved member of
     * the given course.
     *
     * @param student the student to verify
     * @param kurs    the course to check membership in
     * @throws NoSuchElementException if the student is not
     *     an approved member of the course
     */
    private void verifyMembership(
            final Student student,
            final Kurs kurs) {
        findMembership(student, kurs);
    }

    /**
     * Assert that at least one other Kursadmin (other than the
     * given student) remains enrolled in the course.
     *
     * @param kurs             the course to check
     * @param excludedStudent  the student to exclude from the count
     * @throws IllegalStateException if no other Kursadmin would
     *     remain after excluding the given student
     */
    private void verifyMinOneOtherKursadmin(
            final Kurs kurs,
            final Student excludedStudent) {
        long otherKursadminCount = kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs).stream()
            .filter(m -> m.getStudent().getStudentType()
                == StudentType.KURSADMIN)
            .filter(m -> !m.getStudent().getStudentID()
                .equals(excludedStudent.getStudentID()))
            .count();
        if (otherKursadminCount < 1) {
            IllegalStateException ex = new IllegalStateException(
                "At least one KURSADMIN must remain in course");
            colorLogger.colorError(
                "No other Kursadmin remains in course {}: {}",
                kurs.getKursName(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
