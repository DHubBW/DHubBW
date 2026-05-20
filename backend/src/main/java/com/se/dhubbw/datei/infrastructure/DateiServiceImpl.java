package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.application.dto.DateiDTO;
import com.se.dhubbw.datei.application.dto.DateiDownloadDTO;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegung;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
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
import java.util.List;
import java.util.UUID;

/**
 * Service for file management use cases.
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
     * Repository for student lookup.
     */
    private final StudentRepository studentRepository;

    /**
     * Repository for course enrollment lookup.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

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
    @Transactional
    public UUID uploadDatei(
            final MultipartFile file,
            final UUID ueberordnerId,
            final String name,
            final String dhbwMail) {
        Student student = findStudentOrThrow(dhbwMail);
        Ordner ordner = findOrdnerOrThrow(ueberordnerId);
        verifyUploadAuthorized(ordner, student);
        verifyStorageAvailable(file.getSize());

        Datei datei = new Datei();
        datei.setOrdner(ordner);
        datei.setName(StringUtils.cleanPath(name));
        datei.setFileSize((float) (file.getSize() / BYTES_PER_MB));
        datei.setZuletztBearbeitet(LocalDate.now());
        datei.setUploader(student);
        try {
            datei.setData(file.getBytes());
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
    @Transactional
    public void renameDatei(
            final UUID dateiId,
            final String newName,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student student = findStudentOrThrow(dhbwMail);
        verifyModifyAuthorized(datei, student);
        datei.setName(StringUtils.cleanPath(newName));
        datei.setZuletztBearbeitet(LocalDate.now());
        dateiRepository.save(datei);
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
    @Transactional
    public void deleteDatei(
            final UUID dateiId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student student = findStudentOrThrow(dhbwMail);
        verifyModifyAuthorized(datei, student);
        dateiAnlegungRepository.findByDatei(datei)
            .ifPresent(dateiAnlegungRepository::delete);
        dateiRepository.delete(datei);
    }

    /**
     * Assert that the student is allowed to upload to the folder.
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
        if (ordner.getKurs() == null) {
            ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Ordner ist keinem Kurs zugeordnet");
            colorLogger.colorError(
                "Upload rejected – folder {} has no course: {}",
                ordner.getOrdnerID(), ex.getMessage(), ex);
            throw ex;
        }
        if (!isMemberOfKurs(student, ordner.getKurs())) {
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
        if (datei.getOrdner().getKurs() == null) {
            return;
        }
        if (isMemberOfKurs(student, datei.getOrdner().getKurs())) {
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
        Student student = findStudentOrThrow(dhbwMail);
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
        Student student = findStudentOrThrow(dhbwMail);
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
            .map(anfrage -> dateiRepository
                .findByKursIdOrderByNameAsc(
                    anfrage.getKurs().getKursID())
                .stream()
                .map(this::toDateiDto)
                .toList())
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
    @Transactional(readOnly = true)
    public DateiDownloadDTO downloadDatei(
            final UUID dateiId,
            final String dhbwMail) {
        Datei datei = findDateiOrThrow(dateiId);
        Student student = findStudentOrThrow(dhbwMail);
        verifyReadAuthorized(datei.getOrdner(), student);
        return new DateiDownloadDTO(
            datei.getName(), datei.getData());
    }

    /**
     * Assert that the student is allowed to read files in the
     * given folder.
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
        if (ordner.getKurs() == null) {
            return;
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
            datei.getOrdner().getName());
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
     * Look up a student by their DHBW email address.
     *
     * @param dhbwMail the DHBW email address to search for
     * @return the matching student
     * @throws ResponseStatusException 404 if no student is found
     */
    private Student findStudentOrThrow(final String dhbwMail) {
        return studentRepository.findByDhbwMail(dhbwMail)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Student nicht gefunden"));
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
