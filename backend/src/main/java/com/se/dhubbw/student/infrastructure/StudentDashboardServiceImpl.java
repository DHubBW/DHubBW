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
import com.se.dhubbw.student.application.dto.output.PendingKursRequestDTO;
import com.se.dhubbw.student.application.dto.output.StudentProfileDTO;
import com.se.dhubbw.student.domain.StudentDashboardService;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implementation of the {@link StudentDashboardService} interface.
 * Implements course departure, account deletion, and profile retrieval
 * with business rules.
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
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with JPA access)
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
     * Password encoder used to verify the current password and to
     * hash the new password on change.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Color-coded logger for error reporting.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(StudentDashboardServiceImpl.class);

    /**
     * Minimum length required for a new password.
     */
    private static final int MIN_PASSWORD_LENGTH = 10;

    /**
     * Maximum length allowed for a new password.
     */
    private static final int MAX_PASSWORD_LENGTH = 128;

    /**
     * Regex describing the project's password policy. Mirrors the
     * frontend's {@code PASSWORD_REGEX} so client-side and server-side
     * checks stay aligned.
     *
     * <p>Requirements: at least one lowercase letter, one uppercase
     * letter, one digit, one special character (non-letter, non-digit,
     * non-whitespace), 10..128 non-whitespace characters total.</p>
     */
    private static final String PASSWORD_REGEX_STR =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])"
            + "(?=.*[^A-Za-z0-9\\s])\\S{"
            + MIN_PASSWORD_LENGTH + "," + MAX_PASSWORD_LENGTH + "}$";

    /**
     * Compiled form of {@link #PASSWORD_REGEX_STR}.
     */
    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile(PASSWORD_REGEX_STR);

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
     * <p>In addition to name, email, role and course, the profile
     * includes the student's open course request, if any. A pending
     * join request takes precedence over a pending creation request
     * when both exist (edge case that should not occur in normal
     * operation).</p>
     *
     * @param dhbwMail the DHBW mail address of the student
     * @return the student's profile data including name, email,
     *     role, course name, and pending request (if enrolled)
     * @throws NoSuchElementException if the student is not found
     */
    @Override
    @Transactional(readOnly = true)
    public StudentProfileDTO retrieveProfile(final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        var enrollment = kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student);
        UUID kursID = enrollment
            .map(e -> e.getKurs().getKursID())
            .orElse(null);
        String kursName = enrollment
            .map(e -> e.getKurs().getKursName())
            .orElse(null);

        PendingKursRequestDTO pendingKursRequest =
            buildPendingKursRequest(student);

        return new StudentProfileDTO(
            student.getName(),
            student.getDhbwMail(),
            student.getStudentType(),
            kursID,
            kursName,
            pendingKursRequest
        );
    }

    /**
     * Build the pending course request DTO for the given student.
     *
     * <p>Checks for a pending join request first; if none exists,
     * falls back to a pending creation request. Returns {@code null}
     * if the student has no open requests.</p>
     *
     * @param student the student to check
     * @return a {@link PendingKursRequestDTO}, or {@code null}
     */
    private PendingKursRequestDTO buildPendingKursRequest(
            final Student student) {
        return kursBeitrittsAnfrageRepository
            .findAllByStudentAndIsApprovedFalse(student)
            .stream()
            .findFirst()
            .map(req -> new PendingKursRequestDTO(
                "BEITRITT",
                req.getKurs().getKursName(),
                req.getAnfrageDatum()))
            .orElseGet(() ->
                kursErstellungsAnfrageRepository
                    .findByStudentAndIsApprovedFalse(student)
                    .map(req -> new PendingKursRequestDTO(
                        "ERSTELLUNG",
                        req.getKursName(),
                        req.getAnfrageDatum()))
                    .orElse(null));
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

    /**
     * Change the password of the given student.
     *
     * <p>Verifies that {@code currentPassword} matches the stored
     * hash before applying the new value. The new password is
     * validated against the project-wide policy expressed by
     * {@link #PASSWORD_PATTERN} and must differ from the current
     * one.</p>
     *
     * @param dhbwMail the DHBW mail address of the student
     * @param currentPassword the student's current plaintext password
     * @param newPassword the new plaintext password to set
     * @throws NoSuchElementException if the student is not found
     * @throws ResponseStatusException 401 if {@code currentPassword}
     *     is wrong; 400 if {@code newPassword} fails the policy
     *     described by {@link #PASSWORD_PATTERN} or equals
     *     {@code currentPassword}
     */
    @Override
    @RetryableTransactional
    public void changePassword(
            final String dhbwMail,
            final String currentPassword,
            final String newPassword) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);

        if (!passwordEncoder.matches(
                currentPassword, student.getPasswordHash())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Aktuelles Passwort ist falsch");
        }

        if (newPassword == null
                || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Neues Passwort erfüllt die "
                    + "Sicherheitsanforderungen nicht");
        }

        if (newPassword.equals(currentPassword)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Neues Passwort muss sich vom aktuellen "
                    + "unterscheiden");
        }

        student.setPasswordHash(passwordEncoder.encode(newPassword));
        studentRepository.save(student);
        colorLogger.colorInfo("Password changed for {}", dhbwMail);
    }
}
