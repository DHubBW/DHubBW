package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.kurs.domain.KursSystemAdminService;
import com.se.dhubbw.shared.infrastructure.config.retry.RetryableTransactional;
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
 * Implementation of the {@link KursSystemAdminService} interface.
 * Implements course creation request management by the system admin.
 *
 * <p>Provides operations for a system administrator to list,
 * approve, or decline pending course creation requests.
 * On approval, a new course is created and the requesting
 * student is promoted to the {@code KURSADMIN} role with an
 * approved course membership.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with JPA access)
 */
@Service
@RequiredArgsConstructor
public class KursSystemAdminServiceImpl implements KursSystemAdminService {
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

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<KursErstellungsAnfrage>
            retrieveKursErstellungsanfragen() {
        return erstellungsAnfrageRepository.findByIsApprovedFalse();
    }

    /** {@inheritDoc} */
    @Override
    @RetryableTransactional
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

        // Promote to KURSADMIN only if not already SYSTEMADMIN —
        // a SYSTEMADMIN who submitted the request keeps their elevated role.
        if (student.getStudentType() != StudentType.SYSTEMADMIN) {
            student.setStudentType(StudentType.KURSADMIN);
            studentRepository.save(student);
        }

        KursBeitrittsAnfrage beitritt = new KursBeitrittsAnfrage();
        beitritt.setKurs(neuerKurs);
        beitritt.setStudent(student);
        beitritt.setAnfrageDatum(LocalDate.now());
        beitritt.setApproved(true);
        kursBeitrittsAnfrageRepository.save(beitritt);

        anfrage.setApproved(true);
        erstellungsAnfrageRepository.save(anfrage);
    }

    /** {@inheritDoc} */
    @Override
    @RetryableTransactional
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
