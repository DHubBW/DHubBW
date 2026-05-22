package com.se.dhubbw.student.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrageRepository;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegungRepository;
import com.se.dhubbw.shared.infrastructure.config.retry.RetryableTransactional;
import com.se.dhubbw.shared.infrastructure.helper.StudentLookupHelper;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.application.dto.StudentProfileDTO;
import com.se.dhubbw.student.domain.StudentDashboardService;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service implementation for student dashboard operations.
 *
 * <p>Provides operations for students to manage their own
 * course membership and account, with business rules to prevent
 * removing the last admin.</p>
 *
 * <p>Business rules enforced:</p>
 * <ul>
 *   <li>A KURSADMIN cannot leave their course if they are
 *       the last KURSADMIN.</li>
 *   <li>A SYSTEMADMIN cannot delete their account if they are
 *       the last SYSTEMADMIN.</li>
 *   <li>A KURSADMIN cannot delete their account if they are
 *       the last KURSADMIN in their course.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class StudentDashboardServiceImpl implements StudentDashboardService {
    /**
     * Repository for student lookup and deletion.
     */
    private final StudentRepository studentRepository;

    /**
     * Shared helper for student lookup by email.
     */
    private final StudentLookupHelper studentLookupHelper;

    /**
     * Repository for course enrollment management.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Repository for course creation request deletion.
     */
    private final KursErstellungsAnfrageRepository
        kursErstellungsAnfrageRepository;

    /**
     * Repository for file creation record deletion.
     */
    private final DateiAnlegungRepository dateiAnlegungRepository;

    /**
     * Repository for folder creation record deletion.
     */
    private final OrdnerAnlegungRepository ordnerAnlegungRepository;

    /**
     * Repository for file uploader reference updates.
     */
    private final DateiRepository dateiRepository;

    /**
     * Color-coded logger for error reporting.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(StudentDashboardServiceImpl.class);

    /**
     * Remove the student from their current course.
     *
     * <p>Deletes the student's approved course enrollment. If the
     * student is a KURSADMIN, this operation requires that at least
     * one other KURSADMIN remains in the course.</p>
     *
     * @param dhbwMail the DHBW mail address of the student
     * @throws NoSuchElementException if the student is not
     *     found or has no course enrollment
     * @throws IllegalStateException if the student is the last
     *     KURSADMIN in their course
     */
    @Override
    @RetryableTransactional
    public void leaveKurs(final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        KursBeitrittsAnfrage enrollment =
            kursBeitrittsAnfrageRepository
                .findByStudentAndIsApprovedTrue(student)
                .orElseThrow(() -> new NoSuchElementException(
                    "Student has no course enrollment: " + dhbwMail));

        Kurs kurs = enrollment.getKurs();

        // If student is KURSADMIN, verify at least one other remains
        if (student.getStudentType() == StudentType.KURSADMIN) {
            verifyMinOneOtherKursadmin(kurs, student);
        }

        kursBeitrittsAnfrageRepository.delete(enrollment);
        colorLogger.colorInfo("Student {} left course {}",
            dhbwMail, kurs.getKursName());
    }

    /**
     * Delete the student's account completely.
     *
     * <p>Removes the student record and all associated metadata
     * including enrollment requests and creation tracking records.
     * The uploader reference in files is set to null, but files
     * and folders themselves are preserved. This operation fails
     * if the student is the last SYSTEMADMIN or the last KURSADMIN
     * in their course.</p>
     *
     * @param dhbwMail the DHBW mail address of the student
     * @throws NoSuchElementException if the student is not found
     * @throws IllegalStateException if the student is the last
     *     SYSTEMADMIN globally or the last KURSADMIN in their course
     */
    @Override
    @RetryableTransactional
    public void deleteAccount(final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);

        // Check if student is last SYSTEMADMIN
        if (student.getStudentType() == StudentType.SYSTEMADMIN) {
            long systemAdminCount =
                studentRepository.countByStudentType(StudentType.SYSTEMADMIN);
            if (systemAdminCount <= 1) {
                IllegalStateException ex = new IllegalStateException(
                    "Cannot delete account: You are the last SYSTEMADMIN");
                colorLogger.colorError(
                    "Student {} is the last SYSTEMADMIN: {}",
                    dhbwMail, ex.getMessage(), ex);
                throw ex;
            }
        }

        // Check if student is last KURSADMIN in their course
        kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student)
            .ifPresent(enrollment -> {
                Kurs kurs = enrollment.getKurs();
                if (student.getStudentType() == StudentType.KURSADMIN) {
                    verifyMinOneOtherKursadmin(kurs, student);
                }
            });

        // Set file uploader references to null before deleting student
        // Note: Files themselves are NOT deleted
        dateiRepository.setUploaderToNullByUploader(student);

        // Delete associated tracking records
        dateiAnlegungRepository.deleteByStudent(student);
        ordnerAnlegungRepository.deleteByStudent(student);
        kursErstellungsAnfrageRepository.deleteByStudent(student);

        // Delete course enrollment (approved)
        kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student)
            .ifPresent(kursBeitrittsAnfrageRepository::delete);

        // Delete pending join requests
        kursBeitrittsAnfrageRepository
            .deleteAll(kursBeitrittsAnfrageRepository
                .findAllByStudentAndIsApprovedFalse(student));

        // Delete the student account
        studentRepository.delete(student);
        colorLogger.colorInfo("Deleted account for student: {}", dhbwMail);
    }

    /**
     * Assert that at least one other KURSADMIN (other than the
     * given student) remains enrolled in the course.
     *
     * @param kurs             the course to check
     * @param excludedStudent  the student to exclude from the count
     * @throws IllegalStateException if no other KURSADMIN would
     *     remain after excluding the given student
     */
    private void verifyMinOneOtherKursadmin(
            final Kurs kurs,
            final Student excludedStudent) {
        List<KursBeitrittsAnfrage> approvedMembers =
            kursBeitrittsAnfrageRepository
                .findByKursAndIsApprovedTrue(kurs);

        long otherKursadminCount = approvedMembers.stream()
            .filter(m -> m.getStudent().getStudentType()
                == StudentType.KURSADMIN)
            .filter(m -> !m.getStudent().getStudentID()
                .equals(excludedStudent.getStudentID()))
            .count();

        if (otherKursadminCount < 1) {
            IllegalStateException ex = new IllegalStateException(
                "Cannot leave course or delete account: "
                    + "You are the last KURSADMIN in this course");
            colorLogger.colorError(
                "Student {} is the last KURSADMIN in course {}: {}",
                excludedStudent.getDhbwMail(),
                kurs.getKursName(),
                ex.getMessage(),
                ex);
            throw ex;
        }
    }

    /**
     * Retrieve the profile information for the given student.
     *
     * @param dhbwMail the DHBW mail address of the student
     * @return the student's profile data including name, email,
     *     role, and course name (if enrolled)
     * @throws NoSuchElementException if the student is not found
     */
    @Override
    @Transactional(readOnly = true)
    public StudentProfileDTO retrieveProfile(final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        String kursName = kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student)
            .map(enrollment -> enrollment.getKurs().getKursName())
            .orElse(null);

        return new StudentProfileDTO(
            student.getName(),
            student.getDhbwMail(),
            student.getStudentType(),
            kursName
        );
    }

    /**
     * Retrieve the course name for the given student.
     *
     * @param dhbwMail the DHBW mail address of the student
     * @return the course name, or null if the student has no
     *     course enrollment
     * @throws NoSuchElementException if the student is not found
     */
    @Override
    @Transactional(readOnly = true)
    public String retrieveKursForUser(final String dhbwMail) {
        return retrieveProfile(dhbwMail).getKursName();
    }
}
