package com.se.dhubbw.datei.domain;

import com.se.dhubbw.datei.application.dto.output.DateiDTO;
import com.se.dhubbw.datei.application.dto.output.DateiDownloadDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Domain interface for file management.
 * Defines operations for uploading, downloading, renaming, and deleting files.
 *
 * <p>Defines operations for uploading, renaming, deleting,
 * listing, and downloading files.</p>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface, technology-free)
 */
public interface DateiService {

    /**
     * Upload a file and store it in the given folder.
     *
     * @param file          the multipart file to upload
     * @param ueberordnerId the UUID of the target folder
     * @param name          the display name for the stored file
     * @param dhbwMail      the DHBW mail of the authenticated user
     * @return the UUID of the newly created file
     */
    UUID uploadDatei(MultipartFile file,
            UUID ueberordnerId,
            String name,
            String dhbwMail);

    /**
     * Rename an existing file.
     *
     * @param dateiId  the UUID of the file to rename
     * @param newName  the new display name
     * @param dhbwMail the DHBW mail of the authenticated user
     */
    void renameDatei(UUID dateiId,
            String newName,
            String dhbwMail);

    /**
     * Delete a file and its creation record.
     *
     * @param dateiId  the UUID of the file to delete
     * @param dhbwMail the DHBW mail of the authenticated user
     */
    void deleteDatei(UUID dateiId, String dhbwMail);

    /**
     * Retrieve all files in the given folder as DTOs.
     *
     * @param ordnerId the UUID of the folder
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return list of file metadata DTOs, sorted by name
     */
    List<DateiDTO> retrieveDateienForOrdner(
            UUID ordnerId, String dhbwMail);

    /**
     * Retrieve all files of the authenticated user's course.
     *
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return list of file metadata DTOs, sorted by name
     */
    List<DateiDTO> retrieveAllFiles(String dhbwMail);

    /**
     * Retrieve the binary content of a file for download.
     *
     * @param dateiId  the UUID of the file to download
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return DTO containing file name and raw bytes
     */
    DateiDownloadDTO downloadDatei(UUID dateiId, String dhbwMail);

    /**
     * Retrieve the binary content of a file for in-browser preview.
     *
     * <p>Behaves like {@link #downloadDatei(UUID, String)} for
     * authorisation and content retrieval, but does <strong>not</strong>
     * count toward the file's download counter — preview is a
     * read-only inspection, not a download.</p>
     *
     * @param dateiId  the UUID of the file to preview
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return DTO containing file name and raw bytes
     */
    DateiDownloadDTO previewDatei(UUID dateiId, String dhbwMail);

    /**
     * Move a file into another folder.
     *
     * <p>The caller must be modify-authorised on the file's
     * source folder and have upload-rights on the target folder.
     * The target folder must not already contain a file with the
     * same display name; otherwise a 409 conflict is raised.</p>
     *
     * <p>A file's visibility is derived from its parent folder, so
     * moving a file across the public/private boundary changes its
     * visibility implicitly. This is acceptable without an extra
     * confirmation step because the user explicitly picked the
     * target folder.</p>
     *
     * @param dateiId        the UUID of the file to move
     * @param targetOrdnerId the UUID of the target folder
     * @param dhbwMail       the DHBW mail of the authenticated user
     */
    void moveDatei(UUID dateiId, UUID targetOrdnerId, String dhbwMail);
}
