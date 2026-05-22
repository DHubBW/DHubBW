package com.se.dhubbw.ordner.domain;

import com.se.dhubbw.ordner.application.dto.OrdnerDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for folder management use cases.
 *
 * <p>Defines operations to list, create, rename, and delete
 * folders. Access is restricted based on the student's role
 * and course membership.</p>
 */
public interface OrdnerService {

    /**
     * Retrieve all root-level folders for the calling student's
     * course. SYSTEMADMIN receives all root folders across all
     * courses.
     *
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return list of root folders
     * @throws org.springframework.web.server.ResponseStatusException
     *     404 if student not found or non-admin student has no
     *     approved course enrollment
     */
    List<OrdnerDTO> retrieveRootFolders(String dhbwMail);

    /**
     * Retrieve the path from the root folder to the given folder
     * (inclusive, root first).
     *
     * @param folderId the UUID of the target folder
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return ordered list of folders from root to the target
     * @throws org.springframework.web.server.ResponseStatusException
     *     404 if folder or student not found
     * @throws org.springframework.web.server.ResponseStatusException
     *     403 if the caller is not authorised to access this folder
     */
    List<OrdnerDTO> retrieveAllUpperFolders(
            UUID folderId, String dhbwMail);

    /**
     * Retrieve all direct child folders of the given folder.
     *
     * @param folderId the UUID of the parent folder
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return list of direct child folders
     * @throws org.springframework.web.server.ResponseStatusException
     *     404 if folder or student not found
     * @throws org.springframework.web.server.ResponseStatusException
     *     403 if the caller is not authorised to access this folder
     */
    List<OrdnerDTO> retrieveAllDirectFolderChildren(
            UUID folderId, String dhbwMail);

    /**
     * Create a new folder.
     *
     * @param ueberOrdnerId the UUID of the parent folder,
     *     or {@code null} for a root-level folder
     * @param name          the name for the new folder
     * @param isPublic      whether the folder is publicly visible
     * @param farbe         the folder color
     * @param dhbwMail      the DHBW mail of the authenticated user
     * @return the UUID of the newly created folder
     * @throws org.springframework.web.server.ResponseStatusException
     *     404 if parent folder, student, or course not found
     * @throws org.springframework.web.server.ResponseStatusException
     *     403 if not authorised to create a folder here
     */
    UUID createFolder(UUID ueberOrdnerId, String name,
            boolean isPublic, OrdnerFarbe farbe, String dhbwMail);

    /**
     * Rename an existing folder.
     *
     * @param ordnerId the UUID of the folder to rename
     * @param newName  the new name
     * @param dhbwMail the DHBW mail of the authenticated user
     * @throws org.springframework.web.server.ResponseStatusException
     *     404 if folder or student not found
     * @throws org.springframework.web.server.ResponseStatusException
     *     403 if not authorised to rename this folder
     */
    void renameFolder(UUID ordnerId, String newName,
            String dhbwMail);

    /**
     * Delete a folder and all of its contents recursively.
     *
     * @param ordnerId the UUID of the folder to delete
     * @param dhbwMail the DHBW mail of the authenticated user
     * @throws org.springframework.web.server.ResponseStatusException
     *     404 if folder or student not found
     * @throws org.springframework.web.server.ResponseStatusException
     *     403 if not authorised to delete this folder
     */
    void deleteOrdner(UUID ordnerId, String dhbwMail);
}
