package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.application.dto.output.TagDTO;
import com.se.dhubbw.datei.domain.TagService;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.datei.infrastructure.db.Tag;
import com.se.dhubbw.datei.infrastructure.db.TagRepository;
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
 * Implementation of {@link TagService}.
 *
 * <p>Tag names are unique case-insensitively. Creation is gated to
 * KURSADMIN / SYSTEMADMIN; attaching / detaching tags on a file is
 * gated to the file's uploader, KURSADMINs of the file's course, and
 * any SYSTEMADMIN.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with JPA access)
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    /**
     * Repository for tag persistence.
     */
    private final TagRepository tagRepository;

    /**
     * Repository for file lookup (used by the attach / detach flows).
     */
    private final DateiRepository dateiRepository;

    /**
     * Shared helper for student lookups.
     */
    private final StudentLookupHelper studentLookupHelper;

    /**
     * Repository for course enrollment lookup, used by the
     * attach / detach authorisation check.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Color-coded logger for error reporting.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(TagServiceImpl.class);

    /**
     * List all tags, sorted alphabetically.
     *
     * @return list of tag DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<TagDTO> retrieveAllTags() {
        return tagRepository.findAllOrderByNameAsc()
            .stream()
            .map(t -> new TagDTO(t.getTagID(), t.getName()))
            .toList();
    }

    /**
     * Create a new tag.
     *
     * @param name     the display name
     * @param dhbwMail the DHBW mail of the creating student
     * @return the created tag DTO
     * @throws ResponseStatusException 400 if the name is blank or
     *     too long
     * @throws ResponseStatusException 403 if the caller is not a
     *     KURSADMIN / SYSTEMADMIN
     * @throws ResponseStatusException 409 if a tag with the same
     *     name already exists
     */
    @Override
    @RetryableTransactional
    public TagDTO createTag(
            final String name,
            final String dhbwMail) {
        String cleaned = validateName(name);
        Student creator = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        if (creator.getStudentType() != StudentType.KURSADMIN
                && creator.getStudentType() != StudentType.SYSTEMADMIN) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Nur KURSADMIN oder SYSTEMADMIN duerfen Tags anlegen");
            colorLogger.colorError(
                "Tag create rejected for {}: {}",
                dhbwMail, ex.getMessage(), ex);
            throw ex;
        }
        if (tagRepository.findByNameIgnoreCase(cleaned).isPresent()) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ein Tag mit diesem Namen existiert bereits");
            colorLogger.colorError(
                "Tag create conflict for {}: {}",
                cleaned, ex.getMessage(), ex);
            throw ex;
        }
        Tag tag = new Tag();
        tag.setName(cleaned);
        tag.setErsteller(creator);
        tag.setErstelltAm(LocalDateTime.now());
        Tag saved = tagRepository.save(tag);
        return new TagDTO(saved.getTagID(), saved.getName());
    }

    /**
     * Attach an existing tag to a file.
     *
     * @param dateiId  the UUID of the file
     * @param tagId    the UUID of the tag
     * @param dhbwMail the DHBW mail of the requesting student
     * @throws ResponseStatusException 403 if the caller may not
     *     modify the file's tags
     * @throws ResponseStatusException 404 if the file or tag is
     *     not found
     */
    @Override
    @RetryableTransactional
    public void attachTagToDatei(
            final UUID dateiId,
            final UUID tagId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Tag tag = findTagOrThrow(tagId);
        Student requester = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyTagModifyAuthorized(datei, requester);
        datei.getTags().add(tag);
        dateiRepository.save(datei);
    }

    /**
     * Detach a tag from a file.
     *
     * @param dateiId  the UUID of the file
     * @param tagId    the UUID of the tag
     * @param dhbwMail the DHBW mail of the requesting student
     * @throws ResponseStatusException 403 if the caller may not
     *     modify the file's tags
     * @throws ResponseStatusException 404 if the file or tag is
     *     not found
     */
    @Override
    @RetryableTransactional
    public void detachTagFromDatei(
            final UUID dateiId,
            final UUID tagId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Tag tag = findTagOrThrow(tagId);
        Student requester = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyTagModifyAuthorized(datei, requester);
        datei.getTags().removeIf(t ->
            t.getTagID().equals(tag.getTagID()));
        dateiRepository.save(datei);
    }

    /**
     * Permanently delete a tag.
     *
     * <p>{@code SYSTEMADMIN}-only. Detaches the tag from every
     * file that carries it before deleting the row itself, so the
     * {@code datei_tag} join table can never end up with a
     * dangling reference. Files keep their other tags intact.</p>
     *
     * @param tagId    the UUID of the tag to delete
     * @param dhbwMail the DHBW mail of the requesting student
     * @throws ResponseStatusException 403 if the caller is not
     *     {@code SYSTEMADMIN}
     * @throws ResponseStatusException 404 if the tag or student is
     *     not found
     */
    @Override
    @RetryableTransactional
    public void deleteTag(
            final UUID tagId,
            final String dhbwMail) {
        Tag tag = findTagOrThrow(tagId);
        Student requester = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        if (requester.getStudentType() != StudentType.SYSTEMADMIN) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Nur SYSTEMADMIN darf Tags loeschen");
            colorLogger.colorError(
                "Tag delete rejected for {}: {}",
                dhbwMail, ex.getMessage(), ex);
            throw ex;
        }
        // Detach from every file that still holds the chip so the
        // join table is in sync before the row goes away.
        for (Datei datei : new java.util.ArrayList<>(tag.getDateien())) {
            if (datei.getTags() != null) {
                datei.getTags().removeIf(t ->
                    t.getTagID().equals(tag.getTagID()));
                dateiRepository.save(datei);
            }
        }
        tagRepository.delete(tag);
        colorLogger.colorInfo(
            "Tag {} ('{}') deleted by {}",
            tag.getTagID(), tag.getName(), dhbwMail);
    }

    /**
     * Validate a proposed tag name and return the trimmed value.
     *
     * @param name the user-supplied name
     * @return the trimmed, validated name
     * @throws ResponseStatusException 400 if the name is blank or
     *     exceeds {@link Tag#MAX_NAME_LENGTH}
     */
    private String validateName(final String name) {
        if (name == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tag-Name darf nicht leer sein");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tag-Name darf nicht leer sein");
        }
        if (trimmed.length() > Tag.MAX_NAME_LENGTH) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tag-Name ist zu lang");
        }
        return trimmed;
    }

    /**
     * Assert that the caller is authorised to add or remove tags
     * on the given file.
     *
     * <p>Allowed:</p>
     * <ul>
     *   <li>SYSTEMADMIN — always.</li>
     *   <li>The file's uploader.</li>
     *   <li>A KURSADMIN of the file's course.</li>
     * </ul>
     *
     * @param datei     the file whose tags are being modified
     * @param requester the requesting student
     * @throws ResponseStatusException 403 if not authorised
     */
    private void verifyTagModifyAuthorized(
            final Datei datei,
            final Student requester) {
        if (requester.getStudentType() == StudentType.SYSTEMADMIN) {
            return;
        }
        Student uploader = datei.getUploader();
        if (uploader != null
                && uploader.getStudentID()
                    .equals(requester.getStudentID())) {
            return;
        }
        Ordner ordner = datei.getOrdner();
        Kurs kurs = ordner == null ? null : ordner.getKurs();
        if (kurs != null
                && requester.getStudentType()
                    == StudentType.KURSADMIN
                && kursBeitrittsAnfrageRepository
                    .findByStudentAndIsApprovedTrue(requester)
                    .map(a -> a.getKurs().getKursID()
                        .equals(kurs.getKursID()))
                    .orElse(false)) {
            return;
        }
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Keine Berechtigung zur Bearbeitung der Tags");
        colorLogger.colorError(
            "Tag modify rejected for {} on file {}: {}",
            requester.getDhbwMail(),
            datei.getDateiID(),
            ex.getMessage(), ex);
        throw ex;
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
     * Look up a tag by its identifier.
     *
     * @param tagId the tag ID
     * @return the matching tag
     * @throws ResponseStatusException 404 if no tag is found
     */
    private Tag findTagOrThrow(final UUID tagId) {
        return tagRepository.findById(tagId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Tag nicht gefunden"));
    }
}
