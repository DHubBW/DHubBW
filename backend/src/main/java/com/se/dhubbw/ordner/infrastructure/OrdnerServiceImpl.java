package com.se.dhubbw.ordner.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.ordner.application.dto.OrdnerDTO;
import com.se.dhubbw.ordner.domain.OrdnerFarbe;
import com.se.dhubbw.ordner.domain.OrdnerService;
import com.se.dhubbw.shared.infrastructure.config.retry.RetryableTransactional;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegung;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegungRepository;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.shared.infrastructure.helper.StudentLookupHelper;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link OrdnerService} interface.
 * Coordinates folder operations with access control and persistence.
 *
 * <p>Provides operations to list, create, rename, and delete
 * folders. Access is restricted based on the student's role
 * and course membership.</p>
 *
 * <p>Permission rules:</p>
 * <ul>
 *   <li>Read: caller must be a member of the folder's course
 *       or SYSTEMADMIN.</li>
 *   <li>Create: caller must be a member of the target course
 *       or SYSTEMADMIN.</li>
 *   <li>Rename/Delete: caller must be a KURSADMIN of the
 *       folder's course, SYSTEMADMIN, or the original
 *       creator of the folder.</li>
 * </ul>
 *
 * @architecture.component Ordnerverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with JPA access)
 */
@Service
@RequiredArgsConstructor
public class OrdnerServiceImpl implements OrdnerService {
    /**
     * Repository for folder persistence.
     */
    private final OrdnerRepository ordnerRepository;

    /**
     * Repository for folder creation records.
     */
    private final OrdnerAnlegungRepository ordnerAnlegungRepository;

    /**
     * Shared helper for student lookups.
     */
    private final StudentLookupHelper studentLookupHelper;

    /**
     * Repository for course enrollment lookup.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Repository for file persistence.
     */
    private final DateiRepository dateiRepository;

    /**
     * Repository for file creation records.
     */
    private final DateiAnlegungRepository dateiAnlegungRepository;

    /**
     * Color-coded logger for error reporting.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(OrdnerServiceImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrdnerDTO> retrieveRootFolders(
            final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        if (student.getStudentType()
                == StudentType.SYSTEMADMIN) {
            return ordnerRepository
                .findByUeberOrdnerIsNull()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        }
        Kurs kurs = findKursOrThrow(student);
        return ordnerRepository
            .findByKursAndUeberOrdnerIsNull(kurs)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrdnerDTO> retrieveAllUpperFolders(
            final UUID folderId,
            final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        Ordner ordner = findOrdnerOrThrow(folderId);
        verifyCourseAccess(ordner, student);
        List<Ordner> path = new ArrayList<>();
        Ordner current = ordner;
        while (current != null) {
            path.add(0, current);
            current = current.getUeberOrdner();
        }
        return path.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrdnerDTO> retrieveAllDirectFolderChildren(
            final UUID folderId,
            final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        Ordner ordner = findOrdnerOrThrow(folderId);
        verifyCourseAccess(ordner, student);
        return ordnerRepository
            .findChildrenByUeberOrdner(ordner)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RetryableTransactional
    public UUID createFolder(
            final UUID ueberOrdnerId,
            final String name,
            final boolean isPublic,
            final OrdnerFarbe farbe,
            final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        Kurs kurs;
        Ordner ueberOrdner = null;
        int level = 0;
        if (ueberOrdnerId != null) {
            ueberOrdner = findOrdnerOrThrow(ueberOrdnerId);
            verifyCourseAccess(ueberOrdner, student);
            kurs = ueberOrdner.getKurs();
            level = ueberOrdner.getLevel() + 1;
        } else if (student.getStudentType()
                == StudentType.SYSTEMADMIN) {
            kurs = null;
        } else {
            kurs = findKursOrThrow(student);
        }
        Ordner ordner = new Ordner();
        ordner.setUeberOrdner(ueberOrdner);
        ordner.setKurs(kurs);
        ordner.setName(name);
        ordner.setLevel(level);
        ordner.setPublic(isPublic);
        ordner.setFarbe(farbe);
        ordnerRepository.save(ordner);
        OrdnerAnlegung anlegung = new OrdnerAnlegung();
        anlegung.setStudent(student);
        anlegung.setOrdner(ordner);
        anlegung.setDatum(LocalDate.now());
        ordnerAnlegungRepository.save(anlegung);
        return ordner.getOrdnerID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RetryableTransactional
    public void renameFolder(
            final UUID ordnerId,
            final String newName,
            final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        Ordner ordner = findOrdnerOrThrow(ordnerId);
        verifyModifyAuthorized(ordner, student);
        ordner.setName(newName);
        ordnerRepository.save(ordner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RetryableTransactional
    public void deleteOrdner(
            final UUID ordnerId,
            final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        Ordner ordner = findOrdnerOrThrow(ordnerId);
        verifyModifyAuthorized(ordner, student);
        deleteRecursive(ordner);
    }

    /**
     * Recursively delete a folder, its child folders, and all
     * files contained within.
     *
     * @param ordner the folder to delete
     */
    private void deleteRecursive(final Ordner ordner) {
        List<Ordner> children =
            ordnerRepository.findChildrenByUeberOrdner(ordner);
        for (Ordner child : children) {
            deleteRecursive(child);
        }
        List<Datei> dateien = ordner.getDateien();
        if (dateien != null) {
            for (Datei datei : dateien) {
                dateiAnlegungRepository
                    .findByDatei(datei)
                    .ifPresent(dateiAnlegungRepository::delete);
                dateiRepository.delete(datei);
            }
        }
        ordnerAnlegungRepository
            .findByOrdner(ordner)
            .ifPresent(ordnerAnlegungRepository::delete);
        ordnerRepository.delete(ordner);
    }

    /**
     * Assert that the student may access the given folder.
     *
     * <p>SYSTEMADMIN may access any folder. All other users
     * must be a member of the folder's course.</p>
     *
     * @param ordner  the folder to check
     * @param student the authenticated student
     * @throws ResponseStatusException 403 if not authorised
     */
    private void verifyCourseAccess(
            final Ordner ordner,
            final Student student) {
        if (student.getStudentType() == StudentType.SYSTEMADMIN) {
            return;
        }
        Kurs kurs = findKursOrThrow(student);
        if (ordner.getKurs() == null
                || !ordner.getKurs().getKursID()
                    .equals(kurs.getKursID())) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Kein Zugriff auf diesen Ordner");
            colorLogger.colorError(
                "Access denied for {} to folder {}: {}",
                student.getDhbwMail(),
                ordner.getOrdnerID(),
                ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Assert that the student may rename or delete the folder.
     *
     * <p>The caller must be a KURSADMIN of the same course,
     * a SYSTEMADMIN, or the original creator of the folder.</p>
     *
     * @param ordner  the folder to check
     * @param student the authenticated student
     * @throws ResponseStatusException 403 if not authorised
     */
    private void verifyModifyAuthorized(
            final Ordner ordner,
            final Student student) {
        if (student.getStudentType() == StudentType.SYSTEMADMIN) {
            return;
        }
        if (student.getStudentType() == StudentType.KURSADMIN) {
            Kurs studentKurs =
                kursBeitrittsAnfrageRepository
                    .findByStudentAndIsApprovedTrue(student)
                    .map(KursBeitrittsAnfrage::getKurs)
                    .orElse(null);
            if (studentKurs != null
                    && ordner.getKurs() != null
                    && ordner.getKurs().getKursID()
                        .equals(studentKurs.getKursID())) {
                return;
            }
        }
        boolean isCreator = ordnerAnlegungRepository
            .findByOrdner(ordner)
            .map(a -> a.getStudent().getStudentID()
                .equals(student.getStudentID()))
            .orElse(false);
        if (isCreator) {
            return;
        }
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Keine Berechtigung zum Aendern dieses Ordners");
        colorLogger.colorError(
            "Modify denied for {} on folder {}: {}",
            student.getDhbwMail(),
            ordner.getOrdnerID(),
            ex.getMessage(), ex);
        throw ex;
    }

    /**
     * Look up the approved course enrollment of the given
     * student.
     *
     * @param student the student to look up
     * @return the course the student is enrolled in
     * @throws ResponseStatusException 404 if no approved
     *     enrollment is found
     */
    private Kurs findKursOrThrow(final Student student) {
        return kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student)
            .map(KursBeitrittsAnfrage::getKurs)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Kein Kurs gefunden"));
    }

    /**
     * Look up a folder by its identifier.
     *
     * @param ordnerId the folder ID
     * @return the matching folder
     * @throws ResponseStatusException 404 if no folder is found
     */
    private Ordner findOrdnerOrThrow(final UUID ordnerId) {
        return ordnerRepository.findById(ordnerId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Ordner nicht gefunden"));
    }

    /**
     * Map a folder entity to its DTO representation.
     *
     * @param ordner the source entity
     * @return the corresponding DTO
     */
    private OrdnerDTO toDto(final Ordner ordner) {
        UUID ueberOrdnerID = ordner.getUeberOrdner() != null
            ? ordner.getUeberOrdner().getOrdnerID()
            : null;
        UUID kursID = ordner.getKurs() != null
            ? ordner.getKurs().getKursID()
            : null;
        return new OrdnerDTO(
            ordner.getOrdnerID(),
            ordner.getName(),
            ordner.getLevel(),
            ordner.getFarbe(),
            ordner.isPublic(),
            ueberOrdnerID,
            kursID);
    }
}
