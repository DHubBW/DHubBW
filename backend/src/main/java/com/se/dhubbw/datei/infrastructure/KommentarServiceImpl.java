package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.application.dto.output.KommentarDTO;
import com.se.dhubbw.datei.domain.KommentarService;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.datei.infrastructure.db.Kommentar;
import com.se.dhubbw.datei.infrastructure.db.KommentarRepository;
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
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link KommentarService}.
 *
 * <p>Coordinates comment CRUD with access control and persistence.
 * The read/write access rules mirror
 * {@link DateiServiceImpl#verifyReadAuthorized}: a student may read
 * or write a comment exactly when they may see the file itself.
 * Edit authority is limited to the comment author; delete authority
 * is granted to the author, KURSADMINs of the file's course, and
 * any SYSTEMADMIN.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with JPA access)
 */
@Service
@RequiredArgsConstructor
public class KommentarServiceImpl implements KommentarService {

    /**
     * Maximum allowed length of a single comment body in
     * characters. Mirrors the {@code length} attribute on the
     * entity column (kept in sync via
     * {@link Kommentar#MAX_INHALT_LENGTH}).
     */
    private static final int MAX_INHALT_LENGTH =
        Kommentar.MAX_INHALT_LENGTH;

    /**
     * Repository for comment persistence.
     */
    private final KommentarRepository kommentarRepository;

    /**
     * Repository for file lookup (to resolve a comment's parent).
     */
    private final DateiRepository dateiRepository;

    /**
     * Shared helper for student lookups.
     */
    private final StudentLookupHelper studentLookupHelper;

    /**
     * Repository for course enrollment lookup, used to determine
     * whether the requesting student is a member of the file's
     * course.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Color-coded logger for error reporting.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(KommentarServiceImpl.class);

    /**
     * Create a comment on the given file.
     *
     * <p>Authorisation: the requesting student must be allowed to
     * read the file (i.e. anyone who can see the file may also
     * write a comment on it).</p>
     *
     * @param dateiId  the UUID of the file to comment on
     * @param inhalt   the comment text body
     * @param dhbwMail the DHBW mail of the authoring student
     * @return the created comment as a DTO
     * @throws ResponseStatusException 400 if {@code inhalt} is
     *     blank or exceeds the maximum length
     * @throws ResponseStatusException 403 if the user is not
     *     authorised to view the file
     * @throws ResponseStatusException 404 if the file or student
     *     is not found
     */
    @Override
    @RetryableTransactional
    public KommentarDTO createKommentar(
            final UUID dateiId,
            final String inhalt,
            final String dhbwMail) {
        String cleaned = validateInhalt(inhalt);
        Datei datei = findDateiOrThrow(dateiId);
        Student autor = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyReadAuthorized(datei.getOrdner(), autor);

        Kommentar kommentar = new Kommentar();
        kommentar.setDatei(datei);
        kommentar.setAutor(autor);
        kommentar.setInhalt(cleaned);
        kommentar.setErstelltAm(LocalDateTime.now());
        kommentar.setBearbeitet(false);
        kommentar.setZuletztBearbeitet(null);
        Kommentar saved = kommentarRepository.save(kommentar);
        return toKommentarDto(saved, autor);
    }

    /**
     * Retrieve all comments for the given file, oldest first.
     *
     * <p>Authorisation: the requesting student must be allowed to
     * read the file.</p>
     *
     * @param dateiId  the UUID of the file
     * @param dhbwMail the DHBW mail of the requesting student
     * @return list of comment DTOs, may be empty
     * @throws ResponseStatusException 403 if the user is not
     *     authorised to view the file
     * @throws ResponseStatusException 404 if the file or student
     *     is not found
     */
    @Override
    @Transactional(readOnly = true)
    public List<KommentarDTO> retrieveKommentareForDatei(
            final UUID dateiId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student requester = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyReadAuthorized(datei.getOrdner(), requester);
        return kommentarRepository
            .findByDateiIdOrderByErstelltAmAsc(dateiId)
            .stream()
            .map(k -> toKommentarDto(k, requester))
            .toList();
    }

    /**
     * Edit an existing comment.
     *
     * <p>Only the original author may edit. The {@code bearbeitet}
     * flag flips to {@code true} and {@code zuletztBearbeitet} is
     * set to {@code now()}.</p>
     *
     * @param kommentarId the UUID of the comment to edit
     * @param newInhalt   the new comment text body
     * @param dhbwMail    the DHBW mail of the requesting student
     * @return the updated comment as a DTO
     * @throws ResponseStatusException 400 if {@code newInhalt} is
     *     blank or exceeds the maximum length
     * @throws ResponseStatusException 403 if the requester is not
     *     the author of the comment
     * @throws ResponseStatusException 404 if the comment or
     *     student is not found
     */
    @Override
    @RetryableTransactional
    public KommentarDTO updateKommentar(
            final UUID kommentarId,
            final String newInhalt,
            final String dhbwMail) {
        String cleaned = validateInhalt(newInhalt);
        Kommentar kommentar = findKommentarOrThrow(kommentarId);
        Student requester = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyEditAuthorized(kommentar, requester);

        kommentar.setInhalt(cleaned);
        kommentar.setBearbeitet(true);
        kommentar.setZuletztBearbeitet(LocalDateTime.now());
        Kommentar saved = kommentarRepository.save(kommentar);
        return toKommentarDto(saved, requester);
    }

    /**
     * Delete a comment.
     *
     * <p>Allowed for the comment author, KURSADMINs of the file's
     * course, and any SYSTEMADMIN.</p>
     *
     * @param kommentarId the UUID of the comment to delete
     * @param dhbwMail    the DHBW mail of the requesting student
     * @throws ResponseStatusException 403 if the requester is not
     *     authorised to delete this comment
     * @throws ResponseStatusException 404 if the comment or
     *     student is not found
     */
    @Override
    @RetryableTransactional
    public void deleteKommentar(
            final UUID kommentarId,
            final String dhbwMail) {
        Kommentar kommentar = findKommentarOrThrow(kommentarId);
        Student requester = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyDeleteAuthorized(kommentar, requester);
        kommentarRepository.delete(kommentar);
    }

    /**
     * Validate the comment body, returning the trimmed value.
     *
     * @param inhalt the raw comment text body
     * @return the trimmed text body
     * @throws ResponseStatusException 400 if the body is null,
     *     blank, or exceeds {@link #MAX_INHALT_LENGTH}
     */
    private String validateInhalt(final String inhalt) {
        if (inhalt == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Kommentarinhalt darf nicht leer sein");
        }
        String trimmed = inhalt.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Kommentarinhalt darf nicht leer sein");
        }
        if (trimmed.length() > MAX_INHALT_LENGTH) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Kommentar ist zu lang");
        }
        return trimmed;
    }

    /**
     * Assert that the student is allowed to read the given folder.
     *
     * <p>Mirrors the read rules of
     * {@link DateiServiceImpl#verifyReadAuthorized}: SYSTEMADMINs
     * may read everything; public folders are readable by every
     * enrolled student; private folders require course
     * membership.</p>
     *
     * @param ordner  the folder owning the comment's file
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
                throwForbidden(
                    "Read rejected for unenrolled {} on file in"
                        + " folder {}: {}",
                    student, ordner);
            }
            return;
        }
        if (ordner.getKurs() == null) {
            throwForbidden(
                "Read rejected for {} on admin-only folder {}: {}",
                student, ordner);
        }
        if (!isMemberOfKurs(student, ordner.getKurs())) {
            throwForbidden(
                "Read rejected for {} on folder {}: {}",
                student, ordner);
        }
    }

    /**
     * Assert that the requesting student is the author of the
     * comment. Edits may only be performed by the original author.
     *
     * @param kommentar the comment in question
     * @param requester the requesting student
     * @throws ResponseStatusException 403 if the requester is not
     *     the author
     */
    private void verifyEditAuthorized(
            final Kommentar kommentar,
            final Student requester) {
        Student autor = kommentar.getAutor();
        if (autor == null
                || !autor.getStudentID()
                    .equals(requester.getStudentID())) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Nur der Autor darf den Kommentar bearbeiten");
            colorLogger.colorError(
                "Edit rejected for {} on comment {}: {}",
                requester.getDhbwMail(),
                kommentar.getKommentarID(),
                ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Assert that the requesting student may delete the comment.
     *
     * <p>Allowed roles:</p>
     * <ul>
     *   <li>SYSTEMADMIN — always.</li>
     *   <li>The comment author — always.</li>
     *   <li>KURSADMIN of the file's course — when the file's folder
     *       is assigned to a course and the requester is an approved
     *       member of that course with role KURSADMIN.</li>
     * </ul>
     *
     * @param kommentar the comment to delete
     * @param requester the requesting student
     * @throws ResponseStatusException 403 if not authorised
     */
    private void verifyDeleteAuthorized(
            final Kommentar kommentar,
            final Student requester) {
        if (requester.getStudentType()
                == StudentType.SYSTEMADMIN) {
            return;
        }
        Student autor = kommentar.getAutor();
        if (autor != null
                && autor.getStudentID()
                    .equals(requester.getStudentID())) {
            return;
        }
        Datei datei = kommentar.getDatei();
        Ordner ordner = datei == null ? null : datei.getOrdner();
        Kurs kurs = ordner == null ? null : ordner.getKurs();
        if (kurs != null
                && requester.getStudentType()
                    == StudentType.KURSADMIN
                && isMemberOfKurs(requester, kurs)) {
            return;
        }
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Keine Berechtigung zum Loeschen des Kommentars");
        colorLogger.colorError(
            "Delete rejected for {} on comment {}: {}",
            requester.getDhbwMail(),
            kommentar.getKommentarID(),
            ex.getMessage(), ex);
        throw ex;
    }

    /**
     * Throw a 403 with the given log context.
     *
     * @param messageTemplate the log message format string
     * @param student         the rejected student (logged)
     * @param ordner          the folder being accessed (logged)
     */
    private void throwForbidden(
            final String messageTemplate,
            final Student student,
            final Ordner ordner) {
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Kein Zugriff auf diesen Ordner");
        colorLogger.colorError(
            messageTemplate,
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
     * Look up a comment by its identifier.
     *
     * @param kommentarId the comment ID
     * @return the matching comment
     * @throws ResponseStatusException 404 if no comment is found
     */
    private Kommentar findKommentarOrThrow(final UUID kommentarId) {
        return kommentarRepository.findById(kommentarId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Kommentar nicht gefunden"));
    }

    /**
     * Map a {@link Kommentar} entity to a {@link KommentarDTO},
     * computing the requester-specific {@code canEdit} /
     * {@code canDelete} flags.
     *
     * @param kommentar the entity to map
     * @param requester the student requesting the data
     * @return a DTO containing the comment's metadata
     */
    private KommentarDTO toKommentarDto(
            final Kommentar kommentar,
            final Student requester) {
        Student autor = kommentar.getAutor();
        UUID autorID = autor == null ? null : autor.getStudentID();
        String autorName = autor == null ? null : autor.getName();
        boolean isAuthor = autor != null
            && autor.getStudentID()
                .equals(requester.getStudentID());
        boolean canEdit = isAuthor;
        boolean canDelete =
            requester.getStudentType() == StudentType.SYSTEMADMIN
                || isAuthor
                || isKursAdminForKommentar(kommentar, requester);
        return new KommentarDTO(
            kommentar.getKommentarID(),
            kommentar.getDatei() == null
                ? null : kommentar.getDatei().getDateiID(),
            autorID,
            autorName,
            kommentar.getInhalt(),
            kommentar.getErstelltAm(),
            kommentar.getZuletztBearbeitet(),
            kommentar.isBearbeitet(),
            canEdit,
            canDelete);
    }

    /**
     * Check whether the requester is a KURSADMIN for the comment's
     * file course.
     *
     * @param kommentar the comment in question
     * @param requester the requesting student
     * @return {@code true} if the requester is a KURSADMIN of the
     *     file's course
     */
    private boolean isKursAdminForKommentar(
            final Kommentar kommentar,
            final Student requester) {
        if (requester.getStudentType() != StudentType.KURSADMIN) {
            return false;
        }
        Datei datei = kommentar.getDatei();
        if (datei == null) {
            return false;
        }
        Ordner ordner = datei.getOrdner();
        if (ordner == null || ordner.getKurs() == null) {
            return false;
        }
        return isMemberOfKurs(requester, ordner.getKurs());
    }
}
