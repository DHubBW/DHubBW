package com.se.dhubbw.datei.domain;

import com.se.dhubbw.datei.application.dto.DateiDTO;
import com.se.dhubbw.datei.application.dto.DateiDownloadDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for file management use cases.
 *
 * <p>Defines operations for uploading, renaming, deleting,
 * listing, and downloading files.</p>
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
}
