package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegung;
import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.application.dto.KursMitgliedRolleDTO;
import com.se.dhubbw.kurs.application.dto.KursDTO;
import com.se.dhubbw.kurs.application.dto.StudentDTO;
import com.se.dhubbw.kurs.application.dto.SystemAdminStatsDTO;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursRepository;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegung;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegungRepository;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.se.dhubbw.shared.testing.UnitTest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class SystemAdminVerwaltungServiceTest {

    @Mock
    private KursRepository kursRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DateiRepository dateiRepository;

    @Mock
    private OrdnerRepository ordnerRepository;

    @Mock
    private OrdnerAnlegungRepository ordnerAnlegungRepository;

    @Mock
    private DateiAnlegungRepository dateiAnlegungRepository;

    @Mock
    private KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    @Mock
    private KursErstellungsAnfrageRepository
        erstellungsAnfrageRepository;

    @InjectMocks
    private SystemAdminVerwaltungServiceImpl systemAdminVerwaltungService;

    private Kurs kurs;
    private Student student;
    private Student sysAdmin;
    private Student secondSysAdmin;

    @BeforeEach
    public void setUp() {
        kurs = new Kurs();
        kurs.setKursID(UUID.randomUUID());
        kurs.setKursName("TINF24B2");
        kurs.setJahrgang("2024");

        student = new Student();
        student.setStudentID(UUID.randomUUID());
        student.setName("Linus");
        student.setDhbwMail("linus@dhbw.de");
        student.setStudentType(StudentType.STUDENT);

        sysAdmin = new Student();
        sysAdmin.setStudentID(UUID.randomUUID());
        sysAdmin.setName("Tom");
        sysAdmin.setDhbwMail("tom@dhbw.de");
        sysAdmin.setStudentType(StudentType.SYSTEMADMIN);

        secondSysAdmin = new Student();
        secondSysAdmin.setStudentID(UUID.randomUUID());
        secondSysAdmin.setName("Eva");
        secondSysAdmin.setDhbwMail("eva@dhbw.de");
        secondSysAdmin.setStudentType(StudentType.SYSTEMADMIN);
    }

    @Test
    public void givenPlatformData_whenRetrieveStats_thenReturnsStats() {
        when(kursRepository.count()).thenReturn(2L);
        when(studentRepository.count()).thenReturn(10L);
        when(dateiRepository.count()).thenReturn(5L);
        when(dateiRepository.findTotalFileSizeInMb()).thenReturn(3.3);
        when(erstellungsAnfrageRepository.countByIsApprovedFalse())
            .thenReturn(1L);

        SystemAdminStatsDTO stats =
            systemAdminVerwaltungService.retrieveSystemAdminStats();

        assertNotNull(stats);
        assertEquals(2L, stats.getAnzahlKurse());
        assertEquals(10L, stats.getAnzahlNutzer());
        assertEquals(5L, stats.getAnzahlDateien());
        assertEquals(3.3, stats.getMegabyteBelegt(), 0.001);
        assertEquals(1L, stats.getAnzahlKurserstellungsanfragen());
    }

    @Test
    public void givenCourses_whenRetrieveKurse_thenReturnsList() {
        when(kursRepository.findAll()).thenReturn(List.of(kurs));

        List<KursDTO> result =
            systemAdminVerwaltungService.retrieveKurse();

        assertEquals(1, result.size());
        assertEquals(kurs.getKursID(), result.get(0).getKursID());
        assertEquals("TINF24B2", result.get(0).getKursName());
    }

    @Test
    public void givenNoCourses_whenRetrieveKurse_thenReturnsEmpty() {
        when(kursRepository.findAll())
            .thenReturn(Collections.emptyList());

        List<KursDTO> result =
            systemAdminVerwaltungService.retrieveKurse();

        assertEquals(0, result.size());
    }

    @Test
    public void givenKursWithPrivateFolder_whenDelete_thenDeletesFolder() {
        Ordner privateOrdner = new Ordner();
        privateOrdner.setOrdnerID(UUID.randomUUID());
        privateOrdner.setPublic(false);
        privateOrdner.setDateien(Collections.emptyList());

        when(kursRepository.findById(kurs.getKursID()))
            .thenReturn(Optional.of(kurs));
        when(ordnerRepository.findByKurs(kurs))
            .thenReturn(List.of(privateOrdner));
        when(ordnerAnlegungRepository.findByOrdner(privateOrdner))
            .thenReturn(Optional.empty());
        when(kursBeitrittsAnfrageRepository.findByKurs(kurs))
            .thenReturn(Collections.emptyList());

        systemAdminVerwaltungService.deleteKurs(kurs.getKursID());

        verify(ordnerRepository).delete(privateOrdner);
        verify(kursRepository).delete(kurs);
    }

    @Test
    public void givenPrivateFolderWithAnlegung_whenDelete_thenDeletesAnlegung() {
        OrdnerAnlegung anlegung = new OrdnerAnlegung();

        Ordner privateOrdner = new Ordner();
        privateOrdner.setOrdnerID(UUID.randomUUID());
        privateOrdner.setPublic(false);
        privateOrdner.setDateien(Collections.emptyList());

        when(kursRepository.findById(kurs.getKursID()))
            .thenReturn(Optional.of(kurs));
        when(ordnerRepository.findByKurs(kurs))
            .thenReturn(List.of(privateOrdner));
        when(ordnerAnlegungRepository.findByOrdner(privateOrdner))
            .thenReturn(Optional.of(anlegung));
        when(kursBeitrittsAnfrageRepository.findByKurs(kurs))
            .thenReturn(Collections.emptyList());

        systemAdminVerwaltungService.deleteKurs(kurs.getKursID());

        verify(ordnerAnlegungRepository).delete(anlegung);
        verify(ordnerRepository).delete(privateOrdner);
    }

    @Test
    public void givenKursWithPublicFolder_whenDelete_thenDetachesFolder() {
        Ordner publicOrdner = new Ordner();
        publicOrdner.setOrdnerID(UUID.randomUUID());
        publicOrdner.setPublic(true);
        publicOrdner.setDateien(Collections.emptyList());
        publicOrdner.setKurs(kurs);

        when(kursRepository.findById(kurs.getKursID()))
            .thenReturn(Optional.of(kurs));
        when(ordnerRepository.findByKurs(kurs))
            .thenReturn(List.of(publicOrdner));
        when(kursBeitrittsAnfrageRepository.findByKurs(kurs))
            .thenReturn(Collections.emptyList());

        systemAdminVerwaltungService.deleteKurs(kurs.getKursID());

        verify(ordnerRepository, never()).delete(any(Ordner.class));
        verify(ordnerRepository).save(publicOrdner);
        verify(kursRepository).delete(kurs);
    }

    @Test
    public void givenPrivateFolderWithFile_whenDelete_thenDeletesFile() {
        Datei datei = new Datei();
        datei.setDateiID(UUID.randomUUID());

        Ordner privateOrdner = new Ordner();
        privateOrdner.setOrdnerID(UUID.randomUUID());
        privateOrdner.setPublic(false);
        privateOrdner.setDateien(List.of(datei));

        DateiAnlegung anlegung = new DateiAnlegung();
        anlegung.setDatei(datei);

        when(kursRepository.findById(kurs.getKursID()))
            .thenReturn(Optional.of(kurs));
        when(ordnerRepository.findByKurs(kurs))
            .thenReturn(List.of(privateOrdner));
        when(ordnerAnlegungRepository.findByOrdner(privateOrdner))
            .thenReturn(Optional.empty());
        when(dateiAnlegungRepository.findByDatei(datei))
            .thenReturn(Optional.of(anlegung));
        when(kursBeitrittsAnfrageRepository.findByKurs(kurs))
            .thenReturn(Collections.emptyList());

        systemAdminVerwaltungService.deleteKurs(kurs.getKursID());

        verify(dateiAnlegungRepository).delete(anlegung);
        verify(dateiRepository).delete(datei);
        verify(ordnerRepository).delete(privateOrdner);
    }

    @Test
    public void givenKursAdminInKurs_whenDelete_thenDemotesToStudent() {
        Student kursAdmin = new Student();
        kursAdmin.setStudentID(UUID.randomUUID());
        kursAdmin.setStudentType(StudentType.KURSADMIN);

        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setStudent(kursAdmin);
        enrollment.setKurs(kurs);

        when(kursRepository.findById(kurs.getKursID()))
            .thenReturn(Optional.of(kurs));
        when(ordnerRepository.findByKurs(kurs))
            .thenReturn(Collections.emptyList());
        when(kursBeitrittsAnfrageRepository.findByKurs(kurs))
            .thenReturn(List.of(enrollment));

        systemAdminVerwaltungService.deleteKurs(kurs.getKursID());

        assertEquals(StudentType.STUDENT, kursAdmin.getStudentType());
        verify(studentRepository).save(kursAdmin);
        verify(kursBeitrittsAnfrageRepository)
            .deleteAll(List.of(enrollment));
        verify(kursRepository).delete(kurs);
    }

    @Test
    public void givenUnknownKursId_whenDelete_thenThrowsException() {
        UUID unknownId = UUID.randomUUID();
        when(kursRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> systemAdminVerwaltungService.deleteKurs(unknownId));
        verify(kursRepository, never()).delete(any());
    }

    @Test
    public void givenKursWithMembers_whenRetrieveMembers_thenReturnsRoles() {
        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setStudent(student);
        enrollment.setKurs(kurs);

        when(kursRepository.findById(kurs.getKursID()))
            .thenReturn(Optional.of(kurs));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(enrollment));

        List<KursMitgliedRolleDTO> result =
            systemAdminVerwaltungService
                .retrieveKursMembersAndTheirRoles(kurs.getKursID());

        assertEquals(1, result.size());
        assertEquals("Linus", result.get(0).getStudentName());
        assertEquals(StudentType.STUDENT, result.get(0).getRolle());
    }

    @Test
    public void givenUnknownKursId_whenRetrieveMembers_thenThrows() {
        UUID unknownId = UUID.randomUUID();
        when(kursRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> systemAdminVerwaltungService
                .retrieveKursMembersAndTheirRoles(unknownId));
    }

    @Test
    public void givenStudents_whenRetrieveAll_thenReturnsList() {
        when(studentRepository.findAll()).thenReturn(List.of(student));

        List<StudentDTO> result =
            systemAdminVerwaltungService.retrieveAllStudents();

        assertEquals(1, result.size());
        assertEquals(student.getStudentID(),
            result.get(0).getStudentID());
        assertEquals("Linus", result.get(0).getName());
    }

    @Test
    public void givenNoStudents_whenRetrieveAll_thenReturnsEmpty() {
        when(studentRepository.findAll())
            .thenReturn(Collections.emptyList());

        List<StudentDTO> result =
            systemAdminVerwaltungService.retrieveAllStudents();

        assertEquals(0, result.size());
    }

    @Test
    public void givenStudentNotSysAdmin_whenAddSysAdmin_thenPromotes() {
        when(studentRepository.findById(student.getStudentID()))
            .thenReturn(Optional.of(student));

        systemAdminVerwaltungService.addSysAdmin(student.getStudentID());

        assertEquals(StudentType.SYSTEMADMIN,
            student.getStudentType());
        verify(studentRepository).save(student);
    }

    @Test
    public void givenAlreadySysAdmin_whenAddSysAdmin_thenThrowsConflict() {
        when(studentRepository.findById(sysAdmin.getStudentID()))
            .thenReturn(Optional.of(sysAdmin));

        assertThrows(IllegalStateException.class,
            () -> systemAdminVerwaltungService
                .addSysAdmin(sysAdmin.getStudentID()));
        verify(studentRepository, never()).save(any());
    }

    @Test
    public void givenUnknownStudentId_whenAddSysAdmin_thenThrows() {
        UUID unknownId = UUID.randomUUID();
        when(studentRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> systemAdminVerwaltungService.addSysAdmin(unknownId));
    }

    @Test
    public void givenTwoSysAdmins_whenRemoveSysAdmin_thenDemotes() {
        when(studentRepository.findById(sysAdmin.getStudentID()))
            .thenReturn(Optional.of(sysAdmin));
        when(studentRepository
            .countByStudentType(StudentType.SYSTEMADMIN))
            .thenReturn(2L);

        systemAdminVerwaltungService
            .removeSysAdmin(sysAdmin.getStudentID());

        assertEquals(StudentType.STUDENT, sysAdmin.getStudentType());
        verify(studentRepository).save(sysAdmin);
    }

    @Test
    public void givenOneSysAdmin_whenRemoveSysAdmin_thenThrowsConflict() {
        when(studentRepository.findById(sysAdmin.getStudentID()))
            .thenReturn(Optional.of(sysAdmin));
        when(studentRepository
            .countByStudentType(StudentType.SYSTEMADMIN))
            .thenReturn(1L);

        assertThrows(IllegalStateException.class,
            () -> systemAdminVerwaltungService
                .removeSysAdmin(sysAdmin.getStudentID()));
        verify(studentRepository, never()).save(any());
    }

    @Test
    public void givenNotSysAdmin_whenRemoveSysAdmin_thenThrowsBadRequest() {
        when(studentRepository.findById(student.getStudentID()))
            .thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class,
            () -> systemAdminVerwaltungService
                .removeSysAdmin(student.getStudentID()));
    }

    @Test
    public void givenUnknownStudentId_whenRemoveSysAdmin_thenThrows() {
        UUID unknownId = UUID.randomUUID();
        when(studentRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> systemAdminVerwaltungService
                .removeSysAdmin(unknownId));
    }
}
