package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.application.dto.output.DateiBewertungDTO;
import com.se.dhubbw.datei.domain.BewertungService;
import com.se.dhubbw.datei.infrastructure.db.Bewertung;
import com.se.dhubbw.datei.infrastructure.db.BewertungRepository;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.shared.infrastructure.config.retry.RetryableTransactional;
import com.se.dhubbw.shared.infrastructure.helper.StudentLookupHelper;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link BewertungService}.
 *
 * <p>Each {@code (datei, autor)} pair is unique. Setting a rating
 * either inserts a new {@link Bewertung} or updates the existing one
 * — the per-file aggregate held on {@link Datei} is kept consistent
 * inside the same transaction so list views never re-aggregate.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with JPA access)
 */
@Service
@RequiredArgsConstructor
public class BewertungServiceImpl implements BewertungService {

    /**
     * Repository for the rating itself.
     */
    private final BewertungRepository bewertungRepository;

    /**
     * Repository for the file (used to update the aggregate).
     */
    private final DateiRepository dateiRepository;

    /**
     * Shared helper for student lookups.
     */
    private final StudentLookupHelper studentLookupHelper;

    /**
     * Repository for course enrollment lookup, used by the read
     * authorisation check.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Color-coded logger for error reporting.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(BewertungServiceImpl.class);

    /**
     * Set or update the caller's rating.
     *
     * @param dateiId  the UUID of the file
     * @param sterne   star count between 1 and 5
     * @param dhbwMail the DHBW mail of the rating student
     * @return the updated aggregate plus the caller's own rating
     * @throws ResponseStatusException 400 if {@code sterne} is
     *     outside [1, 5]
     * @throws ResponseStatusException 403 if the user is not
     *     authorised to view the file
     * @throws ResponseStatusException 404 if the file or student
     *     is not found
     */
    @Override
    @RetryableTransactional
    public DateiBewertungDTO setBewertung(
            final UUID dateiId,
            final int sterne,
            final String dhbwMail) {
        if (sterne < Bewertung.MIN_STERNE
                || sterne > Bewertung.MAX_STERNE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Sterne muessen zwischen 1 und 5 liegen");
        }
        Datei datei = findDateiOrThrow(dateiId);
        Student autor = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyReadAuthorized(datei.getOrdner(), autor);

        Optional<Bewertung> existing = bewertungRepository
            .findByDateiAndAutor(
                dateiId, autor.getStudentID());
        if (existing.isPresent()) {
            Bewertung b = existing.get();
            int delta = sterne - b.getSterne();
            b.setSterne(sterne);
            b.setBewertetAm(LocalDateTime.now());
            bewertungRepository.save(b);
            datei.setBewertungSumme(
                datei.getBewertungSumme() + delta);
        } else {
            Bewertung b = new Bewertung();
            b.setDatei(datei);
            b.setAutor(autor);
            b.setSterne(sterne);
            b.setBewertetAm(LocalDateTime.now());
            bewertungRepository.save(b);
            datei.setBewertungSumme(
                datei.getBewertungSumme() + sterne);
            datei.setBewertungAnzahl(
                datei.getBewertungAnzahl() + 1);
        }
        dateiRepository.save(datei);
        return toAggregateDto(datei, sterne);
    }

    /**
     * Remove the caller's rating.
     *
     * @param dateiId  the UUID of the file
     * @param dhbwMail the DHBW mail of the rating student
     * @return the updated aggregate plus the caller's own rating
     * @throws ResponseStatusException 404 if the file, student, or
     *     rating is not found
     */
    @Override
    @RetryableTransactional
    public DateiBewertungDTO removeBewertung(
            final UUID dateiId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student autor = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        Optional<Bewertung> existing = bewertungRepository
            .findByDateiAndAutor(
                dateiId, autor.getStudentID());
        if (existing.isEmpty()) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Bewertung nicht gefunden");
            colorLogger.colorError(
                "No rating to remove for {} on file {}: {}",
                dhbwMail, dateiId, ex.getMessage(), ex);
            throw ex;
        }
        Bewertung b = existing.get();
        datei.setBewertungSumme(
            datei.getBewertungSumme() - b.getSterne());
        datei.setBewertungAnzahl(
            datei.getBewertungAnzahl() - 1);
        bewertungRepository.delete(b);
        dateiRepository.save(datei);
        return toAggregateDto(datei, 0);
    }

    /**
     * Retrieve aggregate + caller's own rating.
     *
     * @param dateiId  the UUID of the file
     * @param dhbwMail the DHBW mail of the requesting student
     * @return the aggregate plus the caller's own rating
     */
    @Override
    @Transactional(readOnly = true)
    public DateiBewertungDTO getBewertung(
            final UUID dateiId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student requester = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyReadAuthorized(datei.getOrdner(), requester);
        int eigene = bewertungRepository
            .findByDateiAndAutor(
                dateiId, requester.getStudentID())
            .map(Bewertung::getSterne)
            .orElse(0);
        return toAggregateDto(datei, eigene);
    }

    /**
     * Build the aggregate DTO from the file's cached aggregate
     * and the caller's own rating.
     *
     * @param datei        the rated file
     * @param eigeneSterne the caller's own rating, or {@code 0}
     * @return the aggregate DTO
     */
    private DateiBewertungDTO toAggregateDto(
            final Datei datei,
            final int eigeneSterne) {
        double durchschnitt = datei.getBewertungAnzahl() == 0
            ? 0.0
            : (double) datei.getBewertungSumme()
                / datei.getBewertungAnzahl();
        return new DateiBewertungDTO(
            durchschnitt,
            datei.getBewertungAnzahl(),
            eigeneSterne);
    }

    /**
     * Look up a file by its identifier.
     *
     * @param dateiId the file ID
     * @return the matching file
     * @throws ResponseStatusException 404 if no file is found
     */
    private Datei findDateiOrThrow(final UUID dateiId) {
        return dateiRepository.findById(dateiId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Datei nicht gefunden"));
    }

    /**
     * Mirror of the read-permission rules from
     * {@link DateiServiceImpl}.
     *
     * @param ordner  the folder owning the rated file
     * @param student the requesting student
     * @throws ResponseStatusException 403 if not authorised
     */
    private void verifyReadAuthorized(
            final Ordner ordner,
            final Student student) {
        if (student.getStudentType() == StudentType.SYSTEMADMIN) {
            return;
        }
        if (ordner.isPublic()) {
            boolean hasCourse = kursBeitrittsAnfrageRepository
                .existsByStudentAndIsApprovedTrue(student);
            if (!hasCourse) {
                throwForbidden(student, ordner);
            }
            return;
        }
        if (ordner.getKurs() == null) {
            throwForbidden(student, ordner);
        }
        if (!isMemberOfKurs(student, ordner.getKurs())) {
            throwForbidden(student, ordner);
        }
    }

    /**
     * Throw a 403 with structured logging.
     *
     * @param student the rejected student (logged)
     * @param ordner  the folder being accessed (logged)
     */
    private void throwForbidden(
            final Student student,
            final Ordner ordner) {
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Kein Zugriff auf diese Datei");
        colorLogger.colorError(
            "Bewertung rejected for {} on folder {}: {}",
            student.getDhbwMail(),
            ordner.getOrdnerID(),
            ex.getMessage(), ex);
        throw ex;
    }

    /**
     * Check whether the student has an approved enrollment in the
     * given course.
     *
     * @param student the student to check
     * @param kurs    the course to check against
     * @return {@code true} if the student is a member of the course
     */
    private boolean isMemberOfKurs(
            final Student student,
            final Kurs kurs) {
        return kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student)
            .map(a -> a.getKurs().getKursID()
                .equals(kurs.getKursID()))
            .orElse(false);
    }
}
