package com.se.dhubbw.datei.domain;

import com.se.dhubbw.datei.application.dto.output.TagDTO;

import java.util.List;
import java.util.UUID;

/**
 * Domain interface for tag management.
 *
 * <p>Authorisation rules:</p>
 * <ul>
 *   <li>Listing tags is open to every authenticated student.</li>
 *   <li>Creating new tags is restricted to KURSADMINs and
 *       SYSTEMADMINs — names are unique case-insensitively.</li>
 *   <li>Attaching / detaching tags on a file is restricted to the
 *       file's uploader, KURSADMIN of the file's course, and any
 *       SYSTEMADMIN.</li>
 * </ul>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface, technology-free)
 */
public interface TagService {

    /**
     * List all tags, sorted alphabetically.
     *
     * @return list of tag DTOs
     */
    List<TagDTO> retrieveAllTags();

    /**
     * Create a new tag.
     *
     * @param name     the display name
     * @param dhbwMail the DHBW mail of the creating student
     *     (must be KURSADMIN or SYSTEMADMIN)
     * @return the created tag DTO
     */
    TagDTO createTag(String name, String dhbwMail);

    /**
     * Attach an existing tag to a file.
     *
     * @param dateiId  the UUID of the file
     * @param tagId    the UUID of the tag
     * @param dhbwMail the DHBW mail of the requesting student
     */
    void attachTagToDatei(UUID dateiId, UUID tagId, String dhbwMail);

    /**
     * Detach a tag from a file.
     *
     * @param dateiId  the UUID of the file
     * @param tagId    the UUID of the tag
     * @param dhbwMail the DHBW mail of the requesting student
     */
    void detachTagFromDatei(UUID dateiId, UUID tagId, String dhbwMail);

    /**
     * Permanently delete a tag from the platform.
     *
     * <p>Restricted to {@code SYSTEMADMIN}. Removes the tag's row
     * from the {@code tag} table and every {@code datei_tag} link
     * that referenced it. Tags attached to files lose the chip
     * silently — the files themselves are untouched.</p>
     *
     * @param tagId    the UUID of the tag to delete
     * @param dhbwMail the DHBW mail of the requesting student
     *     (must have role {@code SYSTEMADMIN})
     */
    void deleteTag(UUID tagId, String dhbwMail);
}
