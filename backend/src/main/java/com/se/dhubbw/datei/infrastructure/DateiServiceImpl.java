package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.application.dto.output.DateiDTO;
import com.se.dhubbw.datei.application.dto.output.DateiDownloadDTO;
import com.se.dhubbw.datei.domain.FileStorageStrategy;
import com.se.dhubbw.datei.domain.UploadSecurityValidator;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegung;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.shared.infrastructure.config.retry.RetryableTransactional;
import com.se.dhubbw.shared.infrastructure.helper.StudentLookupHelper;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the {@link com.se.dhubbw.datei.domain.DateiService}
 * interface.
 * Coordinates file operations with access control and persistence.
 *
 * <p>Provides operations to upload, rename, and delete files.
 * Access is restricted based on the student's role and course
 * membership.</p>
 *
 * <p>Permission rules:</p>
 * <ul>
 *   <li>Upload: caller must be a member of the folder's course
 *       or SYSTEMADMIN; platform storage limit must not be
 *       exceeded.</li>
 *   <li>Rename/Delete: caller must be a member of the file's
 *       course or SYSTEMADMIN. If the folder is no longer
 *       assigned to a course, the restriction is waived.</li>
 * </ul>
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with JPA access)
 */
@Service
@RequiredArgsConstructor
public class DateiServiceImpl
    implements com.se.dhubbw.datei.domain.DateiService {

    /**
     * Conversion factor from bytes to megabytes.
     */
    private static final double BYTES_PER_MB = 1_000_000.0;

    /**
     * Repository for file persistence.
     */
    private final DateiRepository dateiRepository;

    /**
     * Repository for file creation records.
     */
    private final DateiAnlegungRepository dateiAnlegungRepository;

    /**
     * Repository for folder lookup.
     */
    private final OrdnerRepository ordnerRepository;

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
     * Strategy for binary file storage (profile-dependent).
     * In dev: in-memory byte array storage.
     * In prod: PostgreSQL Large Object storage.
     */
    private final FileStorageStrategy fileStorageStrategy;

    /**
     * Validator that vets every upload against the platform's
     * security policy (size, type, dangerous extensions).
     */
    private final UploadSecurityValidator uploadSecurityValidator;

    /**
     * Maximum total platform storage in megabytes.
     */
    @Value("${dhubbw.storage.max-mb:1000.0}")
    private double maxStorageMb;

    /**
     * Color-coded logger for error reporting.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(DateiServiceImpl.class);

    /**
     * Upload a file and store it in the given folder.
     *
     * <p>Only a member of the folder's course or a SYSTEMADMIN
     * may upload. The platform storage limit must not be
     * exceeded.</p>
     *
     * @param file          the multipart file to upload
     * @param ueberordnerId the UUID of the target folder
     * @param name          the display name for the stored file
     * @param dhbwMail      the DHBW mail of the authenticated user
     * @return the UUID of the newly created file
     * @throws ResponseStatusException 404 if the folder or student
     *     is not found
     * @throws ResponseStatusException 403 if the user is not
     *     authorised to upload to the given folder
     * @throws ResponseStatusException 507 if the platform storage
     *     limit would be exceeded
     * @throws ResponseStatusException 500 if reading the file
     *     content fails
     */
    @Override
    @RetryableTransactional
    public UUID uploadDatei(
            final MultipartFile file,
            final UUID ueberordnerId,
            final String name,
            final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        Ordner ordner = findOrdnerOrThrow(ueberordnerId);
        verifyUploadAuthorized(ordner, student);
        uploadSecurityValidator.validate(file, name);
        verifyStorageAvailable(file.getSize());
        verifyDateiNameAvailable(ordner, name, null);

        Datei datei = new Datei();
        datei.setOrdner(ordner);
        datei.setName(StringUtils.cleanPath(name));
        datei.setFileSize((float) (file.getSize() / BYTES_PER_MB));
        datei.setZuletztBearbeitet(LocalDate.now());
        datei.setUploader(student);
        try {
            byte[] bytes = file.getBytes();
            long storageRef = fileStorageStrategy.store(bytes);
            datei.setDataOid(storageRef);
            if (fileStorageStrategy.requiresInlineData()) {
                datei.setData(bytes);
            }
        } catch (IOException ex) {
            ResponseStatusException rse = new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Fehler beim Lesen der Datei");
            colorLogger.colorError(
                "Failed to read uploaded file for {}: {}",
                dhbwMail, ex.getMessage(), rse);
            throw rse;
        }
        dateiRepository.save(datei);

        DateiAnlegung anlegung = new DateiAnlegung();
        anlegung.setStudent(student);
        anlegung.setDatei(datei);
        anlegung.setDatum(LocalDate.now());
        dateiAnlegungRepository.save(anlegung);

        return datei.getDateiID();
    }

    /**
     * Rename an existing file.
     *
     * <p>The caller must be a member of the file's course or a
     * SYSTEMADMIN. If the file's folder is no longer associated
     * with a course, the restriction is waived.</p>
     *
     * @param dateiId  the UUID of the file to rename
     * @param newName  the new display name
     * @param dhbwMail the DHBW mail of the authenticated user
     * @throws ResponseStatusException 404 if the file or student
     *     is not found
     * @throws ResponseStatusException 403 if the user is not
     *     authorised to modify the file
     */
    @Override
    @RetryableTransactional
    public void renameDatei(
            final UUID dateiId,
            final String newName,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyModifyAuthorized(datei, student);

        // The user must not be able to change the file extension via
        // rename — the original extension is part of the file's
        // identity (and the security validator already vetted it on
        // upload). Take only the base part of `newName` and re-attach
        // the original extension stored on the existing entity. Empty
        // base ⇒ HTTP 400 so the user knows what went wrong.
        String cleanedNewName = StringUtils.cleanPath(newName);
        String resolvedName = applyOriginalExtension(
            datei.getName(), cleanedNewName);

        verifyDateiNameAvailable(
            datei.getOrdner(), resolvedName, datei.getDateiID());
        datei.setName(resolvedName);
        datei.setZuletztBearbeitet(LocalDate.now());
        dateiRepository.save(datei);
    }

    /**
     * Combine the user-supplied base name with the file's original
     * extension. Both leading/trailing whitespace and any extension
     * the user might have typed are stripped from the input — only
     * the base survives.
     *
     * @param originalName the file's existing name (e.g. {@code
     *     "Skript.pdf"})
     * @param requestedName the new name from the user (may include
     *     an extension that will be discarded)
     * @return the new full name with the original extension reattached
     * @throws ResponseStatusException 400 if the resulting base name
     *     is empty
     */
    private String applyOriginalExtension(
            final String originalName,
            final String requestedName) {
        String trimmed = requestedName == null
            ? "" : requestedName.trim();
        String requestedBase = stripExtension(trimmed);
        if (requestedBase.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Dateiname darf nicht leer sein");
        }
        String originalExt = extensionOf(originalName);
        if (originalExt.isEmpty()) {
            return requestedBase;
        }
        return requestedBase + "." + originalExt;
    }

    /**
     * Returns everything before the final dot in {@code name}, or the
     * whole string when no dot is present. Used by
     * {@link #applyOriginalExtension} to discard whatever extension
     * the user typed.
     *
     * @param name the file name to inspect, may be {@code null}
     * @return the base part of the name (no extension)
     */
    private static String stripExtension(final String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return name;
        }
        return name.substring(0, dot);
    }

    /**
     * Returns the extension after the final dot in {@code name}, or
     * an empty string when the name has no extension. The leading
     * dot is <strong>not</strong> included.
     *
     * @param name the file name to inspect, may be {@code null}
     * @return the extension without the leading dot
     */
    private static String extensionOf(final String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1);
    }

    /**
     * Delete a file and its creation record.
     *
     * <p>The caller must be a member of the file's course or a
     * SYSTEMADMIN. If the file's folder is no longer associated
     * with a course, the restriction is waived.</p>
     *
     * @param dateiId  the UUID of the file to delete
     * @param dhbwMail the DHBW mail of the authenticated user
     * @throws ResponseStatusException 404 if the file or student
     *     is not found
     * @throws ResponseStatusException 403 if the user is not
     *     authorised to delete the file
     */
    @Override
    @RetryableTransactional
    public void deleteDatei(
            final UUID dateiId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyModifyAuthorized(datei, student);
        dateiAnlegungRepository.findByDatei(datei)
            .ifPresent(dateiAnlegungRepository::delete);
        if (datei.getDataOid() != null) {
            fileStorageStrategy.delete(datei.getDataOid());
        }
        dateiRepository.delete(datei);
    }

    /**
     * Move a file into another folder.
     *
     * <p>Permission rules:</p>
     * <ul>
     *   <li>The caller must be modify-authorised on the file's
     *       current folder.</li>
     *   <li>The caller must be upload-authorised on the target
     *       folder. SYSTEMADMINs may always upload; everyone else
     *       must be a member of the target folder's course (or any
     *       course at all when the target folder is public).</li>
     *   <li>The target folder must not already contain a file with
     *       the same display name (HTTP 409).</li>
     * </ul>
     *
     * <p>The file's visibility is derived from its parent folder
     * (public folder ⇒ public file). Moving across the
     * public/private boundary therefore changes visibility
     * implicitly — acceptable here because the caller picked the
     * target folder explicitly.</p>
     *
     * @param dateiId        the UUID of the file to move
     * @param targetOrdnerId the UUID of the target folder
     * @param dhbwMail       the DHBW mail of the authenticated user
     * @throws ResponseStatusException 404 if file, target folder
     *     or student is not found
     * @throws ResponseStatusException 403 if the caller lacks
     *     modify rights on the source folder or upload rights on
     *     the target folder
     * @throws ResponseStatusException 409 if a file with the same
     *     name already exists in the target folder
     * @throws ResponseStatusException 400 if the target folder is
     *     identical to the file's current folder
     */
    @Override
    @RetryableTransactional
    public void moveDatei(
            final UUID dateiId,
            final UUID targetOrdnerId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyModifyAuthorized(datei, student);

        Ordner targetOrdner = findOrdnerOrThrow(targetOrdnerId);
        Ordner sourceOrdner = datei.getOrdner();
        if (sourceOrdner != null
                && sourceOrdner.getOrdnerID()
                    .equals(targetOrdner.getOrdnerID())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Datei liegt bereits in diesem Ordner");
        }
        verifyUploadAuthorized(targetOrdner, student);
        verifyDateiNameAvailable(
            targetOrdner, datei.getName(), datei.getDateiID());

        datei.setOrdner(targetOrdner);
        datei.setZuletztBearbeitet(LocalDate.now());
        dateiRepository.save(datei);
    }

    /**
     * Assert that the student is allowed to upload to the folder.
     *
     * <p>Upload rules:</p>
     * <ul>
     *   <li>SYSTEMADMIN may upload to any folder.</li>
     *   <li>Public folders (including SYSTEMADMIN-created NULL-kurs
     *       platform shares) accept uploads from any student that is
     *       enrolled in some course.</li>
     *   <li>Private folders require the caller to be a member of the
     *       folder's course.</li>
     * </ul>
     *
     * @param ordner  the target folder
     * @param student the authenticated student
     * @throws ResponseStatusException 403 if not authorised
     */
    private void verifyUploadAuthorized(
            final Ordner ordner,
            final Student student) {
        if (student.getStudentType() == StudentType.SYSTEMADMIN) {
            return;
        }
        if (ordner.isPublic()) {
            boolean hasCourse = kursBeitrittsAnfrageRepository
                .existsByStudentAndIsApprovedTrue(student);
            if (!hasCourse) {
                ResponseStatusException ex = new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Kein Zugriff auf diesen Ordner");
                colorLogger.colorError(
                    "Upload rejected for {} in folder {}: {}",
                    student.getDhbwMail(),
                    ordner.getOrdnerID(),
                    ex.getMessage(), ex);
                throw ex;
            }
            return;
        }
        // Private folder: must be a member of the folder's course.
        // A private folder without a course (legacy data only — the
        // service no longer creates such rows) is SYSTEMADMIN-only.
        if (ordner.getKurs() == null
                || !isMemberOfKurs(student, ordner.getKurs())) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Kein Zugriff auf diesen Ordner");
            colorLogger.colorError(
                "Upload rejected for {} in folder {}: {}",
                student.getDhbwMail(),
                ordner.getOrdnerID(),
                ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Assert that the student is allowed to rename or delete
     * the given file.
     *
     * <p>SYSTEMADMIN may modify any file. For files in a public folder
     * with no assigned course (a SYSTEMADMIN-created platform share)
     * any enrolled student may modify the file, mirroring the
     * upload rule. For files in a folder with an assigned course the
     * caller must be a member of that course.</p>
     *
     * @param datei   the file to modify
     * @param student the authenticated student
     * @throws ResponseStatusException 403 if not authorised
     */
    private void verifyModifyAuthorized(
            final Datei datei,
            final Student student) {
        if (student.getStudentType() == StudentType.SYSTEMADMIN) {
            return;
        }
        Ordner ordner = datei.getOrdner();
        if (ordner.getKurs() == null) {
            // Private NULL-kurs folders are not produced by the service
            // (SYSTEMADMINs may only create public platform shares); only
            // the SYSTEMADMIN branch above is allowed to modify their
            // contents — guard defensively against any legacy row.
            if (!ordner.isPublic()) {
                ResponseStatusException ex = new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Kein Zugriff auf diese Datei");
                colorLogger.colorError(
                    "Modify rejected for {} on admin-only file {}: {}",
                    student.getDhbwMail(),
                    datei.getDateiID(),
                    ex.getMessage(), ex);
                throw ex;
            }
            boolean hasCourse = kursBeitrittsAnfrageRepository
                .existsByStudentAndIsApprovedTrue(student);
            if (!hasCourse) {
                ResponseStatusException ex = new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Kein Zugriff auf diese Datei");
                colorLogger.colorError(
                    "Modify rejected for {} on file {}: {}",
                    student.getDhbwMail(),
                    datei.getDateiID(),
                    ex.getMessage(), ex);
                throw ex;
            }
            return;
        }
        if (isMemberOfKurs(student, ordner.getKurs())) {
            return;
        }
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Kein Zugriff auf diese Datei");
        colorLogger.colorError(
            "Modify rejected for {} on file {}: {}",
            student.getDhbwMail(),
            datei.getDateiID(),
            ex.getMessage(), ex);
        throw ex;
    }

    /**
     * Assert that adding the given number of bytes would not
     * exceed the platform storage limit.
     *
     * @param fileSizeBytes the number of bytes to add
     * @throws ResponseStatusException 507 if the limit would be
     *     exceeded
     */
    private void verifyStorageAvailable(final long fileSizeBytes) {
        double fileSizeMb = fileSizeBytes / BYTES_PER_MB;
        double usedMb = dateiRepository.findTotalFileSizeInMb();
        if (usedMb + fileSizeMb > maxStorageMb) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.INSUFFICIENT_STORAGE,
                "Nicht genug Speicherplatz verfuegbar");
            colorLogger.colorError(
                "Storage limit exceeded: used={}, needed={}: {}",
                usedMb, fileSizeMb, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Assert that no other file in the given folder already uses
     * the proposed display name.
     *
     * <p>Enforces per-folder file-name uniqueness for both upload
     * and rename. The {@code excludeDateiId} parameter lets the
     * rename flow skip the row being renamed so a no-op rename is
     * not flagged as a conflict.</p>
     *
     * @param ordner          the target folder
     * @param name            the desired display name (will be
     *     {@link StringUtils#cleanPath cleaned} the same way the
     *     upload / rename writes do)
     * @param excludeDateiId  the file UUID to exclude from the
     *     uniqueness check, or {@code null} to consider every row
     * @throws ResponseStatusException 409 when another file in
     *     the folder already uses the cleaned name
     */
    private void verifyDateiNameAvailable(
            final Ordner ordner,
            final String name,
            final UUID excludeDateiId) {
        String cleaned = StringUtils.cleanPath(name);
        dateiRepository
            .findByOrdnerIdAndName(ordner.getOrdnerID(), cleaned)
            .filter(existing -> excludeDateiId == null
                || !existing.getDateiID().equals(excludeDateiId))
            .ifPresent(existing -> {
                ResponseStatusException ex =
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Eine Datei mit diesem Namen existiert"
                            + " bereits in diesem Ordner");
                colorLogger.colorError(
                    "Datei-Name conflict in folder {} for {}: {}",
                    ordner.getOrdnerID(), cleaned,
                    ex.getMessage(), ex);
                throw ex;
            });
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
     * Retrieve all files in the given folder as DTOs.
     *
     * <p>Only a member of the folder's course or a SYSTEMADMIN
     * may list files. If the folder has no assigned course, the
     * restriction is waived.</p>
     *
     * @param ordnerId the UUID of the folder
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return list of file metadata DTOs, newest first
     * @throws ResponseStatusException 404 if the folder or
     *     student is not found
     * @throws ResponseStatusException 403 if the user is not
     *     authorised to read files in this folder
     */
    @Override
    @Transactional(readOnly = true)
    public List<DateiDTO> retrieveDateienForOrdner(
            final UUID ordnerId,
            final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        Ordner ordner = findOrdnerOrThrow(ordnerId);
        verifyReadAuthorized(ordner, student);
        return dateiRepository
            .findByOrdnerIdOrderByNameAsc(ordnerId)
            .stream()
            .map(this::toDateiDto)
            .toList();
    }

    /**
     * Retrieve all files of the authenticated user's course.
     *
     * <p>SYSTEMADMIN receives all files across all courses.
     * Regular users see only files within their own approved
     * course. Users without an approved course enrollment
     * receive an empty list.</p>
     *
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return list of file metadata DTOs, newest first
     * @throws ResponseStatusException 404 if the student is
     *     not found
     */
    @Override
    @Transactional(readOnly = true)
    public List<DateiDTO> retrieveAllFiles(
            final String dhbwMail) {
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        if (student.getStudentType()
                == StudentType.SYSTEMADMIN) {
            return dateiRepository
                .findAllOrderByNameAsc()
                .stream()
                .map(this::toDateiDto)
                .toList();
        }
        return kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student)
            .map(anfrage -> {
                UUID eigenerKursId = anfrage.getKurs().getKursID();
                List<DateiDTO> own = dateiRepository
                    .findByKursIdOrderByNameAsc(eigenerKursId)
                    .stream()
                    .map(this::toDateiDto)
                    .toList();
                List<DateiDTO> publicOther = dateiRepository
                    .findInPublicRootFoldersExcludingKurs(eigenerKursId)
                    .stream()
                    .map(this::toDateiDto)
                    .toList();
                List<DateiDTO> combined = new ArrayList<>(own);
                combined.addAll(publicOther);
                return combined;
            })
            .orElse(List.of());
    }

    /**
     * Retrieve the binary content of a file for download.
     *
     * <p>The caller must be a member of the file's course or a
     * SYSTEMADMIN. If the file's folder has no assigned course,
     * the restriction is waived.</p>
     *
     * @param dateiId  the UUID of the file to download
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return DTO containing file name and raw bytes
     * @throws ResponseStatusException 404 if the file or
     *     student is not found
     * @throws ResponseStatusException 403 if the user is not
     *     authorised to download the file
     */
    @Override
    @Transactional
    public DateiDownloadDTO downloadDatei(
            final UUID dateiId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyReadAuthorized(datei.getOrdner(), student);
        datei.setDownloadAnzahl(datei.getDownloadAnzahl() + 1);
        dateiRepository.save(datei);

        byte[] data;
        if (datei.getDataOid() != null) {
            data = fileStorageStrategy.retrieve(datei.getDataOid());
        } else {
            data = datei.getData();
        }
        return new DateiDownloadDTO(datei.getName(), data);
    }

    /**
     * Retrieve the binary content of a file for in-browser preview.
     *
     * <p>The caller must be a member of the file's course or a
     * SYSTEMADMIN. If the file's folder has no assigned course,
     * the restriction is waived. The download counter is
     * <strong>not</strong> incremented.</p>
     *
     * @param dateiId  the UUID of the file to preview
     * @param dhbwMail the DHBW mail of the authenticated user
     * @return DTO containing file name and raw bytes
     * @throws ResponseStatusException 404 if the file or
     *     student is not found
     * @throws ResponseStatusException 403 if the user is not
     *     authorised to view the file
     */
    @Override
    @Transactional(readOnly = true)
    public DateiDownloadDTO previewDatei(
            final UUID dateiId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student student = studentLookupHelper
            .findStudentByEmail(dhbwMail);
        verifyReadAuthorized(datei.getOrdner(), student);

        byte[] data;
        if (datei.getDataOid() != null) {
            data = fileStorageStrategy.retrieve(datei.getDataOid());
        } else {
            data = datei.getData();
        }
        return new DateiDownloadDTO(datei.getName(), data);
    }

    /**
     * Assert that the student is allowed to read files in the
     * given folder.
     *
     * <p>Permission rules mirror
     * {@link com.se.dhubbw.ordner.domain.OrdnerService}:</p>
     * <ul>
     *   <li>SYSTEMADMIN may read any folder.</li>
     *   <li>Public folders (including SYSTEMADMIN-created NULL-kurs
     *       platform shares) are readable by every student that is
     *       enrolled in some course; unenrolled students are rejected.
     *       </li>
     *   <li>Private folders require course membership; private folders
     *       without an assigned course are SYSTEMADMIN-only.</li>
     * </ul>
     *
     * @param ordner  the folder whose files are being accessed
     * @param student the authenticated student
     * @throws ResponseStatusException 403 if not authorised
     */
    private void verifyReadAuthorized(
            final Ordner ordner,
            final Student student) {
        if (student.getStudentType()
                == StudentType.SYSTEMADMIN) {
            return;
        }
        if (ordner.isPublic()) {
            boolean hasCourse = kursBeitrittsAnfrageRepository
                .existsByStudentAndIsApprovedTrue(student);
            if (!hasCourse) {
                ResponseStatusException ex = new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Kein Zugriff auf diesen Ordner");
                colorLogger.colorError(
                    "Read rejected for unenrolled {} in folder {}: {}",
                    student.getDhbwMail(),
                    ordner.getOrdnerID(),
                    ex.getMessage(), ex);
                throw ex;
            }
            return;
        }
        if (ordner.getKurs() == null) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Kein Zugriff auf diesen Ordner");
            colorLogger.colorError(
                "Read rejected for {} in admin-only folder {}: {}",
                student.getDhbwMail(),
                ordner.getOrdnerID(),
                ex.getMessage(), ex);
            throw ex;
        }
        if (isMemberOfKurs(student, ordner.getKurs())) {
            return;
        }
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Kein Zugriff auf diesen Ordner");
        colorLogger.colorError(
            "Read rejected for {} in folder {}: {}",
            student.getDhbwMail(),
            ordner.getOrdnerID(),
            ex.getMessage(), ex);
        throw ex;
    }

    /**
     * Map a {@link Datei} entity to a {@link DateiDTO}.
     *
     * @param datei the entity to map
     * @return a DTO containing the file's metadata
     */
    private DateiDTO toDateiDto(final Datei datei) {
        String uploaderName = null;
        if (datei.getUploader() != null) {
            uploaderName = datei.getUploader().getName();
        }
        return new DateiDTO(
            datei.getDateiID(),
            datei.getName(),
            datei.getZuletztBearbeitet(),
            datei.getFileSize(),
            uploaderName,
            datei.getOrdner().getOrdnerID(),
            datei.getOrdner().getName(),
            datei.getDownloadAnzahl());
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
     * Look up a folder by its identifier.
     *
     * @param ordnerId the folder ID
     * @return the matching folder
     * @throws ResponseStatusException 404 if no folder is found
     */
    private Ordner findOrdnerOrThrow(final UUID ordnerId) {
        return ordnerRepository.findById(ordnerId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Ordner nicht gefunden"));
    }
}
