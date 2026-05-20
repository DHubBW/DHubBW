package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegung;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.datei.application.dto.DateiDTO;
import com.se.dhubbw.datei.application.dto.DateiDownloadDTO;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DateiServiceTest {

    @Mock
    private DateiRepository dateiRepository;

    @Mock
    private DateiAnlegungRepository dateiAnlegungRepository;

    @Mock
    private OrdnerRepository ordnerRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    @InjectMocks
    private DateiServiceImpl dateiService;

    @Mock
    private MultipartFile mockFile;

    private Student kursAdmin;
    private Student sysAdmin;
    private Student regularStudent;
    private Kurs kurs;
    private Ordner ordnerWithKurs;
    private Ordner ordnerWithoutKurs;
    private Datei datei;

    @BeforeEach
    public void setUp() throws IOException {
        kurs = new Kurs();
        kurs.setKursID(UUID.randomUUID());
        kurs.setKursName("TINF24B2");

        kursAdmin = new Student();
        kursAdmin.setStudentID(UUID.randomUUID());
        kursAdmin.setDhbwMail("admin@dhbw.de");
        kursAdmin.setStudentType(StudentType.KURSADMIN);

        sysAdmin = new Student();
        sysAdmin.setStudentID(UUID.randomUUID());
        sysAdmin.setDhbwMail("sys@dhbw.de");
        sysAdmin.setStudentType(StudentType.SYSTEMADMIN);

        regularStudent = new Student();
        regularStudent.setStudentID(UUID.randomUUID());
        regularStudent.setDhbwMail("student@dhbw.de");
        regularStudent.setStudentType(StudentType.STUDENT);

        ordnerWithKurs = new Ordner();
        ordnerWithKurs.setOrdnerID(UUID.randomUUID());
        ordnerWithKurs.setKurs(kurs);

        ordnerWithoutKurs = new Ordner();
        ordnerWithoutKurs.setOrdnerID(UUID.randomUUID());
        ordnerWithoutKurs.setKurs(null);

        datei = new Datei();
        datei.setDateiID(UUID.randomUUID());
        datei.setOrdner(ordnerWithKurs);
        datei.setName("test.pdf");

        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        ReflectionTestUtils.setField(
            dateiService, "maxStorageMb", 1000.0);
    }

    // --- uploadDatei tests ---

    @Test
    public void givenKursMember_whenUpload_thenReturnsDateiId() {
        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setKurs(kurs);

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(ordnerRepository.findById(ordnerWithKurs.getOrdnerID()))
            .thenReturn(Optional.of(ordnerWithKurs));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(kursAdmin))
            .thenReturn(Optional.of(enrollment));
        when(dateiRepository.findTotalFileSizeInMb()).thenReturn(0.0);
        when(dateiRepository.save(any(Datei.class)))
            .thenAnswer(inv -> {
                Datei d = inv.getArgument(0);
                d.setDateiID(UUID.randomUUID());
                return d;
            });

        UUID result = dateiService.uploadDatei(
            mockFile,
            ordnerWithKurs.getOrdnerID(),
            "test.pdf",
            "admin@dhbw.de");

        assertEquals(datei.getDateiID().getClass(), result.getClass());
        verify(dateiRepository).save(any(Datei.class));
        verify(dateiAnlegungRepository).save(any(DateiAnlegung.class));
    }

    @Test
    public void givenStudentMember_whenUpload_thenReturnsDateiId() {
        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setKurs(kurs);

        when(studentRepository.findByDhbwMail("student@dhbw.de"))
            .thenReturn(Optional.of(regularStudent));
        when(ordnerRepository.findById(ordnerWithKurs.getOrdnerID()))
            .thenReturn(Optional.of(ordnerWithKurs));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(regularStudent))
            .thenReturn(Optional.of(enrollment));
        when(dateiRepository.findTotalFileSizeInMb()).thenReturn(0.0);
        when(dateiRepository.save(any(Datei.class)))
            .thenAnswer(inv -> {
                Datei d = inv.getArgument(0);
                d.setDateiID(UUID.randomUUID());
                return d;
            });

        UUID result = dateiService.uploadDatei(
            mockFile,
            ordnerWithKurs.getOrdnerID(),
            "test.pdf",
            "student@dhbw.de");

        assertEquals(datei.getDateiID().getClass(), result.getClass());
        verify(dateiRepository).save(any(Datei.class));
    }

    @Test
    public void givenSysAdmin_whenUpload_thenSucceeds() {
        when(studentRepository.findByDhbwMail("sys@dhbw.de"))
            .thenReturn(Optional.of(sysAdmin));
        when(ordnerRepository.findById(ordnerWithKurs.getOrdnerID()))
            .thenReturn(Optional.of(ordnerWithKurs));
        when(dateiRepository.findTotalFileSizeInMb()).thenReturn(0.0);
        when(dateiRepository.save(any(Datei.class)))
            .thenAnswer(inv -> {
                Datei d = inv.getArgument(0);
                d.setDateiID(UUID.randomUUID());
                return d;
            });

        dateiService.uploadDatei(
            mockFile,
            ordnerWithKurs.getOrdnerID(),
            "test.pdf",
            "sys@dhbw.de");

        verify(dateiRepository).save(any(Datei.class));
    }

    @Test
    public void givenStudentNotFound_whenUpload_thenThrows404() {
        when(studentRepository.findByDhbwMail("unknown@dhbw.de"))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.uploadDatei(
                mockFile,
                ordnerWithKurs.getOrdnerID(),
                "test.pdf",
                "unknown@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenOrdnerNotFound_whenUpload_thenThrows404() {
        UUID unknownId = UUID.randomUUID();
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(ordnerRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.uploadDatei(
                mockFile, unknownId, "test.pdf", "admin@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenNonMember_whenUpload_thenThrows403() {
        when(studentRepository.findByDhbwMail("student@dhbw.de"))
            .thenReturn(Optional.of(regularStudent));
        when(ordnerRepository.findById(ordnerWithKurs.getOrdnerID()))
            .thenReturn(Optional.of(ordnerWithKurs));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.uploadDatei(
                mockFile,
                ordnerWithKurs.getOrdnerID(),
                "test.pdf",
                "student@dhbw.de"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    public void givenOrdnerNoKurs_whenUpload_thenThrows403() {
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(ordnerRepository.findById(
            ordnerWithoutKurs.getOrdnerID()))
            .thenReturn(Optional.of(ordnerWithoutKurs));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.uploadDatei(
                mockFile,
                ordnerWithoutKurs.getOrdnerID(),
                "test.pdf",
                "admin@dhbw.de"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    public void givenStorageFull_whenUpload_thenThrows507() {
        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setKurs(kurs);

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(ordnerRepository.findById(ordnerWithKurs.getOrdnerID()))
            .thenReturn(Optional.of(ordnerWithKurs));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(kursAdmin))
            .thenReturn(Optional.of(enrollment));
        when(dateiRepository.findTotalFileSizeInMb())
            .thenReturn(1000.0);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.uploadDatei(
                mockFile,
                ordnerWithKurs.getOrdnerID(),
                "test.pdf",
                "admin@dhbw.de"));

        assertEquals(507, ex.getStatusCode().value());
    }

    // --- renameDatei tests ---

    @Test
    public void givenMember_whenRename_thenRenames() {
        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setKurs(kurs);

        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(kursAdmin))
            .thenReturn(Optional.of(enrollment));

        dateiService.renameDatei(
            datei.getDateiID(), "newName.pdf", "admin@dhbw.de");

        verify(dateiRepository).save(datei);
        assertEquals("newName.pdf", datei.getName());
    }

    @Test
    public void givenSysAdmin_whenRename_thenRenames() {
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("sys@dhbw.de"))
            .thenReturn(Optional.of(sysAdmin));

        dateiService.renameDatei(
            datei.getDateiID(), "newName.pdf", "sys@dhbw.de");

        verify(dateiRepository).save(datei);
    }

    @Test
    public void givenStudentMember_whenRename_thenRenames() {
        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setKurs(kurs);

        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("student@dhbw.de"))
            .thenReturn(Optional.of(regularStudent));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(regularStudent))
            .thenReturn(Optional.of(enrollment));

        dateiService.renameDatei(
            datei.getDateiID(), "newName.pdf", "student@dhbw.de");

        verify(dateiRepository).save(datei);
    }

    @Test
    public void givenNonMember_whenRename_thenThrows403() {
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("student@dhbw.de"))
            .thenReturn(Optional.of(regularStudent));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.renameDatei(
                datei.getDateiID(), "new.pdf", "student@dhbw.de"));

        assertEquals(403, ex.getStatusCode().value());
        verify(dateiRepository, never()).save(any());
    }

    @Test
    public void givenNoKurs_whenRename_thenAllowed() {
        datei.setOrdner(ordnerWithoutKurs);

        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("student@dhbw.de"))
            .thenReturn(Optional.of(regularStudent));

        dateiService.renameDatei(
            datei.getDateiID(), "new.pdf", "student@dhbw.de");

        verify(dateiRepository).save(datei);
    }

    @Test
    public void givenDateiNotFound_whenRename_thenThrows404() {
        UUID unknownId = UUID.randomUUID();
        when(dateiRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.renameDatei(
                unknownId, "new.pdf", "admin@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenStudentNotFound_whenRename_thenThrows404() {
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("unknown@dhbw.de"))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.renameDatei(
                datei.getDateiID(), "new.pdf", "unknown@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    // --- deleteDatei tests ---

    @Test
    public void givenMember_whenDelete_thenDeletes() {
        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setKurs(kurs);

        DateiAnlegung anlegung = new DateiAnlegung();
        anlegung.setStudent(kursAdmin);

        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(kursAdmin))
            .thenReturn(Optional.of(enrollment));
        when(dateiAnlegungRepository.findByDatei(datei))
            .thenReturn(Optional.of(anlegung));

        dateiService.deleteDatei(datei.getDateiID(), "admin@dhbw.de");

        verify(dateiAnlegungRepository).delete(anlegung);
        verify(dateiRepository).delete(datei);
    }

    @Test
    public void givenSysAdmin_whenDelete_thenDeletes() {
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("sys@dhbw.de"))
            .thenReturn(Optional.of(sysAdmin));
        when(dateiAnlegungRepository.findByDatei(datei))
            .thenReturn(Optional.empty());

        dateiService.deleteDatei(datei.getDateiID(), "sys@dhbw.de");

        verify(dateiRepository).delete(datei);
    }

    @Test
    public void givenNonMember_whenDelete_thenThrows403() {
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("student@dhbw.de"))
            .thenReturn(Optional.of(regularStudent));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.deleteDatei(
                datei.getDateiID(), "student@dhbw.de"));

        assertEquals(403, ex.getStatusCode().value());
        verify(dateiRepository, never()).delete(any(Datei.class));
    }

    @Test
    public void givenDateiNotFound_whenDelete_thenThrows404() {
        UUID unknownId = UUID.randomUUID();
        when(dateiRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.deleteDatei(
                unknownId, "admin@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenStudentNotFound_whenDelete_thenThrows404() {
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("unknown@dhbw.de"))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.deleteDatei(
                datei.getDateiID(), "unknown@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    // --- retrieveDateienForOrdner tests ---

    @Test
    public void givenMember_whenRetrieveDateienForOrdner_thenReturnsDtos() {
        KursBeitrittsAnfrage enrollment =
            new KursBeitrittsAnfrage();
        enrollment.setKurs(kurs);
        UUID ordnerId = ordnerWithKurs.getOrdnerID();

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(ordnerRepository.findById(ordnerId))
            .thenReturn(Optional.of(ordnerWithKurs));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(kursAdmin))
            .thenReturn(Optional.of(enrollment));
        when(dateiRepository
            .findByOrdnerIdOrderByNameAsc(ordnerId))
            .thenReturn(List.of(datei));

        List<DateiDTO> result =
            dateiService.retrieveDateienForOrdner(
                ordnerId, "admin@dhbw.de");

        assertEquals(1, result.size());
        assertEquals(
            datei.getDateiID(),
            result.get(0).getDateiID());
    }

    @Test
    public void givenSysAdmin_whenRetrieveDateienForOrdner_thenSucceeds() {
        UUID ordnerId = ordnerWithKurs.getOrdnerID();

        when(studentRepository.findByDhbwMail("sys@dhbw.de"))
            .thenReturn(Optional.of(sysAdmin));
        when(ordnerRepository.findById(ordnerId))
            .thenReturn(Optional.of(ordnerWithKurs));
        when(dateiRepository
            .findByOrdnerIdOrderByNameAsc(ordnerId))
            .thenReturn(List.of(datei));

        List<DateiDTO> result =
            dateiService.retrieveDateienForOrdner(
                ordnerId, "sys@dhbw.de");

        assertEquals(1, result.size());
    }

    @Test
    public void givenOrdnerNotFound_whenRetrieveDateienForOrdner_thenThrows404() {
        UUID unknownId = UUID.randomUUID();
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(ordnerRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.retrieveDateienForOrdner(
                unknownId, "admin@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenNonMember_whenRetrieveDateienForOrdner_thenThrows403() {
        UUID ordnerId = ordnerWithKurs.getOrdnerID();
        when(studentRepository.findByDhbwMail("student@dhbw.de"))
            .thenReturn(Optional.of(regularStudent));
        when(ordnerRepository.findById(ordnerId))
            .thenReturn(Optional.of(ordnerWithKurs));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(regularStudent))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.retrieveDateienForOrdner(
                ordnerId, "student@dhbw.de"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    public void givenStudentNotFound_whenRetrieveDateienForOrdner_thenThrows404() {
        UUID ordnerId = ordnerWithKurs.getOrdnerID();
        when(studentRepository.findByDhbwMail("unknown@dhbw.de"))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.retrieveDateienForOrdner(
                ordnerId, "unknown@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenOrdnerNoKurs_whenRetrieveDateienForOrdner_thenAllowed() {        UUID ordnerId = ordnerWithoutKurs.getOrdnerID();
        when(studentRepository.findByDhbwMail("student@dhbw.de"))
            .thenReturn(Optional.of(regularStudent));
        when(ordnerRepository.findById(ordnerId))
            .thenReturn(Optional.of(ordnerWithoutKurs));
        when(dateiRepository
            .findByOrdnerIdOrderByNameAsc(ordnerId))
            .thenReturn(List.of());

        List<DateiDTO> result =
            dateiService.retrieveDateienForOrdner(
                ordnerId, "student@dhbw.de");

        assertTrue(result.isEmpty());
    }

    // --- retrieveAllFiles tests ---

    @Test
    public void givenMember_whenRetrieveAllFiles_thenReturnsKursDtos() {
        KursBeitrittsAnfrage enrollment =
            new KursBeitrittsAnfrage();
        enrollment.setKurs(kurs);

        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(kursAdmin))
            .thenReturn(Optional.of(enrollment));
        when(dateiRepository
            .findByKursIdOrderByNameAsc(
                kurs.getKursID()))
            .thenReturn(List.of(datei));

        List<DateiDTO> result =
            dateiService.retrieveAllFiles("admin@dhbw.de");

        assertEquals(1, result.size());
    }

    @Test
    public void givenNoEnrollment_whenRetrieveAllFiles_thenReturnsEmpty() {
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(kursAdmin))
            .thenReturn(Optional.empty());

        List<DateiDTO> result =
            dateiService.retrieveAllFiles("admin@dhbw.de");

        assertTrue(result.isEmpty());
    }

    @Test
    public void givenSysAdmin_whenRetrieveAllFiles_thenReturnsAllFiles() {
        when(studentRepository.findByDhbwMail("sys@dhbw.de"))
            .thenReturn(Optional.of(sysAdmin));
        when(dateiRepository
            .findAllOrderByNameAsc())
            .thenReturn(List.of(datei));

        List<DateiDTO> result =
            dateiService.retrieveAllFiles("sys@dhbw.de");

        assertEquals(1, result.size());
    }

    @Test
    public void givenStudentNotFound_whenRetrieveAllFiles_thenThrows404() {
        when(studentRepository.findByDhbwMail("unknown@dhbw.de"))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.retrieveAllFiles("unknown@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    // --- downloadDatei tests ---

    @Test
    public void givenMember_whenDownloadDatei_thenReturnsDto() {
        KursBeitrittsAnfrage enrollment =
            new KursBeitrittsAnfrage();
        enrollment.setKurs(kurs);
        datei.setData(new byte[]{1, 2, 3});

        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(kursAdmin));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(kursAdmin))
            .thenReturn(Optional.of(enrollment));

        DateiDownloadDTO result = dateiService.downloadDatei(
            datei.getDateiID(), "admin@dhbw.de");

        assertNotNull(result);
        assertEquals("test.pdf", result.getName());
    }

    @Test
    public void givenSysAdmin_whenDownloadDatei_thenSucceeds() {
        datei.setData(new byte[]{1, 2, 3});

        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("sys@dhbw.de"))
            .thenReturn(Optional.of(sysAdmin));

        DateiDownloadDTO result = dateiService.downloadDatei(
            datei.getDateiID(), "sys@dhbw.de");

        assertNotNull(result);
    }

    @Test
    public void givenNonMember_whenDownloadDatei_thenThrows403() {
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("student@dhbw.de"))
            .thenReturn(Optional.of(regularStudent));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(regularStudent))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.downloadDatei(
                datei.getDateiID(), "student@dhbw.de"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    public void givenDateiNotFound_whenDownloadDatei_thenThrows404() {
        UUID unknownId = UUID.randomUUID();
        when(dateiRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.downloadDatei(
                unknownId, "admin@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenStudentNotFound_whenDownloadDatei_thenThrows404() {
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("unknown@dhbw.de"))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> dateiService.downloadDatei(
                datei.getDateiID(), "unknown@dhbw.de"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenOrdnerNoKurs_whenDownloadDatei_thenAllowed() {
        datei.setOrdner(ordnerWithoutKurs);
        datei.setData(new byte[]{1, 2, 3});

        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(studentRepository.findByDhbwMail("student@dhbw.de"))
            .thenReturn(Optional.of(regularStudent));

        DateiDownloadDTO result = dateiService.downloadDatei(
            datei.getDateiID(), "student@dhbw.de");

        assertNotNull(result);
    }
}
