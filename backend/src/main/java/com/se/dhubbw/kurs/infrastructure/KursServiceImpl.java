package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.kurs.application.dto.KursYearDTO;
import com.se.dhubbw.kurs.domain.KursService;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursRepository;
import com.se.dhubbw.shared.infrastructure.config.retry.RetryableTransactional;
import com.se.dhubbw.shared.infrastructure.helper.StudentLookupHelper;
import com.se.dhubbw.student.infrastructure.db.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the {@link KursService} interface.
 * Implements course queries, join requests, and course creation requests
 * with validation.
 *
 * <p>Implements the business rules for retrieving courses,
 * opening join requests, and opening course creation
 * requests.</p>
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>A student may be enrolled in at most one course.</li>
 *   <li>A student may have at most one open join request.</li>
 *   <li>A course creation request requires that no course
 *       with the same name and cohort year already exists.</li>
 * </ul>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with JPA access)
 */
@Service
@RequiredArgsConstructor
public class KursServiceImpl implements KursService {
    /**
     * Default notes value when no additional notes are provided.
     */
    private static final String DEFAULT_NOTES = "No notes were provided.";

    /**
     * Repository for querying and persisting course entities.
     */
    private final KursRepository kursRepository;

    /**
     * Repository for querying and persisting join requests.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Repository for querying and persisting creation requests.
     */
    private final KursErstellungsAnfrageRepository
        erstellungsAnfrageRepository;

    /**
     * Shared helper for student lookups.
     */
    private final StudentLookupHelper studentLookupHelper;

    /**
     * Retrieve all available courses with their identifier,
     * name, and cohort year.
     *
     * @return list of all courses; may be empty
     */
    @Transactional(readOnly = true)
    public List<KursYearDTO> retrieveKursNamesAndYears() {
        return kursRepository.findAll().stream()
            .map(k -> new KursYearDTO(
                k.getKursID(), k.getKursName(), k.getJahrgang()))
            .toList();
    }

    /**
     * Open a join request for the given student and course.
     *
     * <p>Validates that the course exists, the student is not
     * already enrolled in any course, and the student has no
     * other open join request.</p>
     *
     * @param kursId   the UUID of the course to join
     * @param dhbwMail the DHBW email of the requesting student
     * @throws ResponseStatusException 404 if course or student
     *     is not found
     * @throws ResponseStatusException 409 if the student is
     *     already enrolled or has an open join request
     */
    @RetryableTransactional
    @Override
    public void openJoinKursRequest(final UUID kursId, final String dhbwMail) {

        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);

        Kurs kurs = kursRepository.findById(kursId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Kurs nicht gefunden"));

        if (kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(student)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Student ist bereits einem Kurs zugeordnet");
        }

        if (kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedFalse(student)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Student hat bereits eine offene Beitrittsanfrage");
        }

        KursBeitrittsAnfrage anfrage = new KursBeitrittsAnfrage();
        anfrage.setKurs(kurs);
        anfrage.setStudent(student);
        anfrage.setAnfrageDatum(LocalDate.now());
        anfrage.setApproved(false);
        kursBeitrittsAnfrageRepository.save(anfrage);
    }

    /**
     * Open a course creation request for the given student.
     *
     * <p>Validates that the student is not currently enrolled
     * in any course, has no open creation request, and that no
     * course with the same name and cohort year already
     * exists.</p>
     *
     * @param kursName        desired name of the new course
     * @param jahrgang        desired cohort year (e.g. "2024")
     * @param additionalNotes optional notes;
     *     {@code null} is replaced by a default message
     * @param dhbwMail        DHBW email of the requesting student
     * @throws ResponseStatusException 404 if the student is
     *     not found
     * @throws ResponseStatusException 409 if the student is
     *     already enrolled, already has an open creation
     *     request, or if a course with the same name and
     *     cohort year already exists
     */
    @RetryableTransactional
    @Override
    public void openCreateCourseRequest(final String kursName,
            final String jahrgang, final String additionalNotes,
            final String dhbwMail) {

        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);

        if (kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(student)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Student ist bereits einem Kurs zugeordnet");
        }

        if (erstellungsAnfrageRepository
            .existsByStudentAndIsApprovedFalse(student)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Student hat bereits eine offene Kurserstellungsanfrage");
        }

        if (kursRepository.existsByKursNameAndJahrgang(
            kursName, jahrgang)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ein Kurs mit diesem Namen und Jahrgang existiert"
                    + " bereits");
        }

        if (!kursName.matches("[A-Z]{2,4}[0-9]{2}[A-Z][0-9]")) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Der gewählte Name folgt nicht dem erforderlichen"
                    + " Format (z.B. 'TINF24B2')"
            );
        }

        String notes = additionalNotes != null
            ? additionalNotes : DEFAULT_NOTES;

        KursErstellungsAnfrage anfrage = new KursErstellungsAnfrage();
        anfrage.setStudent(student);
        anfrage.setKursName(kursName);
        anfrage.setJahrgang(jahrgang);
        anfrage.setAnfrageDatum(LocalDate.now());
        anfrage.setApproved(false);
        anfrage.setRequestNotes(notes);
        erstellungsAnfrageRepository.save(anfrage);
    }
}
