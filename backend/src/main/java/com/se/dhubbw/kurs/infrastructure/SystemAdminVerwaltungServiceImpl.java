package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.application.dto.KursMitgliedRolleDTO;
import com.se.dhubbw.kurs.application.dto.KursDTO;
import com.se.dhubbw.kurs.application.dto.StudentDTO;
import com.se.dhubbw.kurs.application.dto.SystemAdminStatsDTO;
import com.se.dhubbw.kurs.domain.SystemAdminVerwaltungService;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursRepository;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegungRepository;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service for system-admin platform management.
 *
 * <p>Provides operations for a SYSTEMADMIN to retrieve platform
 * statistics, list and delete courses (with full cascade),
 * list course members with their roles, list all students, and
 * manage the SYSTEMADMIN role.</p>
 *
 * <p>Business rules enforced:</p>
 * <ul>
 *   <li>Course deletion cascades through folders, files, folder
 *       creation records, file creation records, and
 *       enrollments.</li>
 *   <li>KURSADMIN students of a deleted course are demoted to
 *       STUDENT.</li>
 *   <li>At least one SYSTEMADMIN must remain on the platform
 *       after a removal.</li>
 *   <li>A student cannot be promoted to SYSTEMADMIN if they
 *       already hold that role.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SystemAdminVerwaltungServiceImpl
    implements SystemAdminVerwaltungService {

    /**
     * Repository for course operations.
     */
    private final KursRepository kursRepository;

    /**
     * Repository for student lookup and role updates.
     */
    private final StudentRepository studentRepository;

    /**
     * Repository for file operations.
     */
    private final DateiRepository dateiRepository;

    /**
     * Repository for folder operations.
     */
    private final OrdnerRepository ordnerRepository;

    /**
     * Repository for folder creation records.
     */
    private final OrdnerAnlegungRepository ordnerAnlegungRepository;

    /**
     * Repository for file creation records.
     */
    private final DateiAnlegungRepository dateiAnlegungRepository;

    /**
     * Repository for course enrollment operations.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Repository for course creation request operations.
     */
    private final KursErstellungsAnfrageRepository
        erstellungsAnfrageRepository;

    /**
     * Maximum total storage allowed for file uploads, in MB.
     */
    @Value("${dhubbw.storage.max-mb:1000.0}")
    private double maxStorageMb;

    /**
     * Color-coded logger for error reporting.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(SystemAdminVerwaltungServiceImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public SystemAdminStatsDTO retrieveSystemAdminStats() {
        long anzahlKurse = kursRepository.count();
        long anzahlNutzer = studentRepository.count();
        long anzahlDateien = dateiRepository.count();
        double megabyteBelegt = dateiRepository.findTotalFileSizeInMb();
        double megabyteVerfuegbar = maxStorageMb - megabyteBelegt;
        long anzahlKurserstellungsanfragen =
            erstellungsAnfrageRepository.countByIsApprovedFalse();
        return new SystemAdminStatsDTO(
            anzahlKurse,
            anzahlNutzer,
            anzahlDateien,
            megabyteBelegt,
            megabyteVerfuegbar,
            anzahlKurserstellungsanfragen);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<KursDTO> retrieveKurse() {
        return kursRepository.findAll().stream()
            .map(kurs -> new KursDTO(
                kurs.getKursID(),
                kurs.getKursName(),
                kurs.getJahrgang()))
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteKurs(final UUID kursId) {
        Kurs kurs = findKursOrThrow(kursId);

        List<Ordner> ordnerListe = ordnerRepository.findByKurs(kurs);
        for (Ordner ordner : ordnerListe) {
            if (ordner.isPublic()) {
                ordner.setKurs(null);
                ordnerRepository.save(ordner);
            } else {
                deleteAllDateienInOrdner(ordner);
                ordnerAnlegungRepository
                    .findByOrdner(ordner)
                    .ifPresent(ordnerAnlegungRepository::delete);
                ordnerRepository.delete(ordner);
            }
        }

        List<KursBeitrittsAnfrage> enrollments =
            kursBeitrittsAnfrageRepository.findByKurs(kurs);
        for (KursBeitrittsAnfrage enrollment : enrollments) {
            Student member = enrollment.getStudent();
            if (member.getStudentType() == StudentType.KURSADMIN) {
                member.setStudentType(StudentType.STUDENT);
                studentRepository.save(member);
            }
        }
        kursBeitrittsAnfrageRepository.deleteAll(enrollments);

        kursRepository.delete(kurs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<KursMitgliedRolleDTO> retrieveKursMembersAndTheirRoles(
            final UUID kursId) {
        Kurs kurs = findKursOrThrow(kursId);
        return kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs).stream()
            .map(enrollment -> new KursMitgliedRolleDTO(
                enrollment.getStudent().getName(),
                enrollment.getStudent().getStudentType()))
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<StudentDTO> retrieveAllStudents() {
        return studentRepository.findAll().stream()
            .map(student -> new StudentDTO(
                student.getStudentID(),
                student.getName(),
                student.getDhbwMail(),
                student.getStudentType()))
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void addSysAdmin(final UUID studentId) {
        Student student = findStudentByIdOrThrow(studentId);
        if (student.getStudentType() == StudentType.SYSTEMADMIN) {
            IllegalStateException ex = new IllegalStateException(
                "Student is already SYSTEMADMIN");
            colorLogger.colorError(
                "Attempted to promote existing SYSTEMADMIN {}: {}",
                student.getDhbwMail(), ex.getMessage(), ex);
            throw ex;
        }
        student.setStudentType(StudentType.SYSTEMADMIN);
        studentRepository.save(student);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void removeSysAdmin(final UUID studentId) {
        Student student = findStudentByIdOrThrow(studentId);
        if (student.getStudentType() != StudentType.SYSTEMADMIN) {
            IllegalArgumentException ex = new IllegalArgumentException(
                "Student is not a SYSTEMADMIN");
            colorLogger.colorError(
                "Attempted to demote non-SYSTEMADMIN {}: {}",
                student.getDhbwMail(), ex.getMessage(), ex);
            throw ex;
        }
        verifyMinOneOtherSysAdmin(student);
        student.setStudentType(StudentType.STUDENT);
        studentRepository.save(student);
    }

    /**
     * Delete all files and their creation records within a folder.
     *
     * @param ordner the folder whose files to delete
     */
    private void deleteAllDateienInOrdner(final Ordner ordner) {
        List<Datei> dateien = ordner.getDateien();
        if (dateien == null) {
            return;
        }
        for (Datei datei : dateien) {
            dateiAnlegungRepository
                .findByDatei(datei)
                .ifPresent(dateiAnlegungRepository::delete);
            dateiRepository.delete(datei);
        }
    }

    /**
     * Assert that at least one other SYSTEMADMIN (other than the
     * given student) remains registered on the platform.
     *
     * @param excludedStudent the SYSTEMADMIN being demoted
     * @throws IllegalStateException if no other SYSTEMADMIN would
     *     remain after the operation
     */
    private void verifyMinOneOtherSysAdmin(
            final Student excludedStudent) {
        long countSysAdmins = studentRepository
            .countByStudentType(StudentType.SYSTEMADMIN);
        if (countSysAdmins <= 1) {
            IllegalStateException ex = new IllegalStateException(
                "At least one SYSTEMADMIN must remain on the platform");
            colorLogger.colorError(
                "Cannot remove last SYSTEMADMIN {}: {}",
                excludedStudent.getDhbwMail(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Find a course by its identifier.
     *
     * @param kursId the course ID
     * @return the matching course
     * @throws NoSuchElementException if no course with that ID
     *     exists
     */
    private Kurs findKursOrThrow(final UUID kursId) {
        return kursRepository.findById(kursId)
            .orElseThrow(() -> new NoSuchElementException(
                "Kurs not found: " + kursId));
    }

    /**
     * Find a student by their identifier.
     *
     * @param studentId the student ID
     * @return the matching student
     * @throws NoSuchElementException if no student with that ID
     *     exists
     */
    private Student findStudentByIdOrThrow(final UUID studentId) {
        return studentRepository.findById(studentId)
            .orElseThrow(() -> new NoSuchElementException(
                "Student not found: " + studentId));
    }
}
