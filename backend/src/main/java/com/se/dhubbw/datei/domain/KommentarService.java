package com.se.dhubbw.datei.domain;

import com.se.dhubbw.datei.application.dto.output.KommentarDTO;

import java.util.List;
import java.util.UUID;

/**
 * Domain interface for file-comment management.
 *
 * <p>Defines operations to create, list, edit, and delete comments
 * attached to a file. Authorisation rules:</p>
 * <ul>
 *   <li>Reading comments and writing a new comment requires the
 *       same access as reading the file itself (same rules as
 *       {@link DateiService#previewDatei}).</li>
 *   <li>Editing a comment is restricted to its author.</li>
 *   <li>Deleting a comment is allowed for the author, the
 *       owning folder's KURSADMIN, and any SYSTEMADMIN.</li>
 * </ul>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface, technology-free)
 */
public interface KommentarService {

    /**
     * Create a new comment on the given file.
     *
     * @param dateiId  the UUID of the file to comment on
     * @param inhalt   the comment text body
     * @param dhbwMail the DHBW mail of the authoring student
     * @return the created comment as a DTO
     */
    KommentarDTO createKommentar(UUID dateiId,
            String inhalt,
            String dhbwMail);

    /**
     * Retrieve all comments belonging to the given file, ordered
     * oldest first.
     *
     * @param dateiId  the UUID of the file
     * @param dhbwMail the DHBW mail of the requesting student
     * @return list of comment DTOs, oldest first
     */
    List<KommentarDTO> retrieveKommentareForDatei(UUID dateiId,
            String dhbwMail);

    /**
     * Edit an existing comment.
     *
     * <p>Only the original author may edit. On success the
     * comment's {@code bearbeitet} flag flips to {@code true} and
     * the modification timestamp is recorded.</p>
     *
     * @param kommentarId the UUID of the comment to edit
     * @param newInhalt   the new comment text body
     * @param dhbwMail    the DHBW mail of the requesting student
     * @return the updated comment as a DTO
     */
    KommentarDTO updateKommentar(UUID kommentarId,
            String newInhalt,
            String dhbwMail);

    /**
     * Delete a comment.
     *
     * <p>Allowed for the author, the file's KURSADMIN, and any
     * SYSTEMADMIN.</p>
     *
     * @param kommentarId the UUID of the comment to delete
     * @param dhbwMail    the DHBW mail of the requesting student
     */
    void deleteKommentar(UUID kommentarId, String dhbwMail);
}
