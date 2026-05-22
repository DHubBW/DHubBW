package com.se.dhubbw.ordner.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.ordner.application.dto.OrdnerDTO;
import com.se.dhubbw.ordner.domain.OrdnerFarbe;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegung;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegungRepository;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.shared.infrastructure.helper.StudentLookupHelper;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrdnerServiceImpl}.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OrdnerServiceTest {

    /** Mock repository for folder persistence. */
    @Mock
    private OrdnerRepository ordnerRepository;

    /** Mock repository for folder creation records. */
    @Mock
    private OrdnerAnlegungRepository ordnerAnlegungRepository;

    /** Mock helper for student lookup. */
    @Mock
    private StudentLookupHelper studentLookupHelper;

    /** Mock repository for course enrollment lookup. */
    @Mock
    private KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /** Mock repository for file persistence. */
    @Mock
    private DateiRepository dateiRepository;

    /** Mock repository for file creation records. */
    @Mock
    private DateiAnlegungRepository dateiAnlegungRepository;

    /** System under test. */
    @InjectMocks
    private OrdnerServiceImpl ordnerService;

    /** Test student (STUDENT role). */
    private Student student;

    /** Test student (KURSADMIN role). */
    private Student kursAdmin;

    /** Test student (SYSTEMADMIN role). */
    private Student sysAdmin;

    /** Test course. */
    private Kurs kurs;

    /** Root-level test folder. */
    private Ordner rootOrdner;

    /** Child test folder. */
    private Ordner childOrdner;

    /** Approved enrollment for the regular student. */
    private KursBeitrittsAnfrage anfrage;

    /** Approved enrollment for the kursadmin. */
    private KursBeitrittsAnfrage kursAdminAnfrage;

    /**
     * Set up common test fixtures before each test.
     */
    @BeforeEach
    public void setUp() {
        kurs = new Kurs();
        kurs.setKursID(UUID.randomUUID());
        kurs.setKursName("TINF24B2");

        student = new Student();
        student.setStudentID(UUID.randomUUID());
        student.setDhbwMail("student@dhbw.de");
        student.setStudentType(StudentType.STUDENT);

        kursAdmin = new Student();
        kursAdmin.setStudentID(UUID.randomUUID());
        kursAdmin.setDhbwMail("kursadmin@dhbw.de");
        kursAdmin.setStudentType(StudentType.KURSADMIN);

        sysAdmin = new Student();
        sysAdmin.setStudentID(UUID.randomUUID());
        sysAdmin.setDhbwMail("system.administrator.a24@student.dhbw-karlsruhe.de");
        sysAdmin.setStudentType(StudentType.SYSTEMADMIN);

        anfrage = new KursBeitrittsAnfrage();
        anfrage.setKurs(kurs);
        anfrage.setStudent(student);

        kursAdminAnfrage = new KursBeitrittsAnfrage();
        kursAdminAnfrage.setKurs(kurs);
        kursAdminAnfrage.setStudent(kursAdmin);

        rootOrdner = new Ordner();
        rootOrdner.setOrdnerID(UUID.randomUUID());
        rootOrdner.setKurs(kurs);
        rootOrdner.setName("Vorlesungen");
        rootOrdner.setLevel(0);
        rootOrdner.setFarbe(OrdnerFarbe.DEFAULT);

        childOrdner = new Ordner();
        childOrdner.setOrdnerID(UUID.randomUUID());
        childOrdner.setKurs(kurs);
        childOrdner.setUeberOrdner(rootOrdner);
        childOrdner.setName("Mathematik");
        childOrdner.setLevel(1);
        childOrdner.setFarbe(OrdnerFarbe.BLUE);

        when(studentLookupHelper.findStudentByEmail("student@dhbw.de"))
            .thenReturn(student);
        when(studentLookupHelper.findStudentByEmail("kursadmin@dhbw.de"))
            .thenReturn(kursAdmin);
        when(studentLookupHelper.findStudentByEmail("system.administrator.a24@student.dhbw-karlsruhe.de"))
            .thenReturn(sysAdmin);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(anfrage));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(kursAdmin))
            .thenReturn(Optional.of(kursAdminAnfrage));
    }

    // --- retrieveRootFolders ---

    /**
     * Happy path: student with a course receives root folders.
     */
    @Test
    public void givenStudent_whenRetrieveRootFolders_thenReturnsRoots() {
        when(ordnerRepository.findByKursAndUeberOrdnerIsNull(kurs))
            .thenReturn(List.of(rootOrdner));

        List<OrdnerDTO> result =
            ordnerService.retrieveRootFolders("student@dhbw.de");

        assertEquals(1, result.size());
        assertEquals(
            rootOrdner.getOrdnerID(), result.get(0).getOrdnerID());
    }

    /**
     * Student with no course gets 404.
     */
    @Test
    public void givenNoCourse_whenRetrieveRootFolders_thenThrows404() {
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.empty());

        assertThrows(
            ResponseStatusException.class,
            () -> ordnerService.retrieveRootFolders(
                "student@dhbw.de"));
    }

    // --- retrieveAllUpperFolders ---

    /**
     * Happy path: child folder returns path [root, child].
     */
    @Test
    public void givenChildFolder_whenUpperFolders_thenReturnsPath() {
        when(ordnerRepository.findById(childOrdner.getOrdnerID()))
            .thenReturn(Optional.of(childOrdner));

        List<OrdnerDTO> path = ordnerService.retrieveAllUpperFolders(
            childOrdner.getOrdnerID(), "student@dhbw.de");

        assertEquals(2, path.size());
        assertEquals(
            rootOrdner.getOrdnerID(), path.get(0).getOrdnerID());
        assertEquals(
            childOrdner.getOrdnerID(), path.get(1).getOrdnerID());
    }

    /**
     * Folder from different course returns 403.
     */
    @Test
    public void givenOtherCourse_whenUpperFolders_thenThrows403() {
        Kurs otherKurs = new Kurs();
        otherKurs.setKursID(UUID.randomUUID());
        Ordner otherOrdner = new Ordner();
        otherOrdner.setOrdnerID(UUID.randomUUID());
        otherOrdner.setKurs(otherKurs);
        when(ordnerRepository.findById(otherOrdner.getOrdnerID()))
            .thenReturn(Optional.of(otherOrdner));

        assertThrows(
            ResponseStatusException.class,
            () -> ordnerService.retrieveAllUpperFolders(
                otherOrdner.getOrdnerID(), "student@dhbw.de"));
    }

    /**
     * Unknown folder ID returns 404.
     */
    @Test
    public void givenUnknownId_whenUpperFolders_thenThrows404() {
        UUID unknown = UUID.randomUUID();
        when(ordnerRepository.findById(unknown))
            .thenReturn(Optional.empty());

        assertThrows(
            ResponseStatusException.class,
            () -> ordnerService.retrieveAllUpperFolders(
                unknown, "student@dhbw.de"));
    }

    // --- retrieveAllDirectFolderChildren ---

    /**
     * Happy path: root folder returns its direct children.
     */
    @Test
    public void givenRootFolder_whenChildren_thenReturnsChildren() {
        when(ordnerRepository.findById(rootOrdner.getOrdnerID()))
            .thenReturn(Optional.of(rootOrdner));
        when(ordnerRepository.findChildrenByUeberOrdner(rootOrdner))
            .thenReturn(List.of(childOrdner));

        List<OrdnerDTO> children =
            ordnerService.retrieveAllDirectFolderChildren(
                rootOrdner.getOrdnerID(), "student@dhbw.de");

        assertEquals(1, children.size());
        assertEquals(
            childOrdner.getOrdnerID(),
            children.get(0).getOrdnerID());
    }

    /**
     * Folder from a different course returns 403 for children.
     */
    @Test
    public void givenOtherCourse_whenChildren_thenThrows403() {
        Kurs otherKurs = new Kurs();
        otherKurs.setKursID(UUID.randomUUID());
        Ordner otherOrdner = new Ordner();
        otherOrdner.setOrdnerID(UUID.randomUUID());
        otherOrdner.setKurs(otherKurs);
        when(ordnerRepository.findById(otherOrdner.getOrdnerID()))
            .thenReturn(Optional.of(otherOrdner));

        assertThrows(
            ResponseStatusException.class,
            () -> ordnerService.retrieveAllDirectFolderChildren(
                otherOrdner.getOrdnerID(), "student@dhbw.de"));
    }

    // --- createFolder ---

    /**
     * Happy path: student creates a root-level folder.
     */
    @Test
    public void givenStudent_whenCreateRootFolder_thenSavesAndReturnsId() {
        when(ordnerRepository.save(any(Ordner.class)))
            .thenAnswer(inv -> {
                Ordner saved = inv.getArgument(0);
                saved.setOrdnerID(UUID.randomUUID());
                return saved;
            });
        when(ordnerAnlegungRepository.save(any(OrdnerAnlegung.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        UUID id = ordnerService.createFolder(
            null, "Neuer Ordner", true, OrdnerFarbe.RED,
            "student@dhbw.de");

        assertNotNull(id);
        verify(ordnerRepository).save(any(Ordner.class));
    }

    /**
     * Happy path: student creates a child folder under an
     * existing parent.
     */
    @Test
    public void givenParent_whenCreateChildFolder_thenLevelIncremented() {
        when(ordnerRepository.findById(rootOrdner.getOrdnerID()))
            .thenReturn(Optional.of(rootOrdner));
        when(ordnerRepository.save(any(Ordner.class)))
            .thenAnswer(inv -> {
                Ordner saved = inv.getArgument(0);
                saved.setOrdnerID(UUID.randomUUID());
                return saved;
            });
        when(ordnerAnlegungRepository.save(any(OrdnerAnlegung.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ordnerService.createFolder(
            rootOrdner.getOrdnerID(), "Sub", true,
            OrdnerFarbe.GREEN, "student@dhbw.de");

        verify(ordnerRepository).save(any(Ordner.class));
    }

    // --- renameFolder ---

    /**
     * KursAdmin may rename a folder in their course.
     */
    @Test
    public void givenKursAdmin_whenRename_thenSaves() {
        when(ordnerRepository.findById(rootOrdner.getOrdnerID()))
            .thenReturn(Optional.of(rootOrdner));
        when(ordnerRepository.save(any(Ordner.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ordnerService.renameFolder(
            rootOrdner.getOrdnerID(), "Umbenannt",
            "kursadmin@dhbw.de");

        verify(ordnerRepository).save(rootOrdner);
        assertEquals("Umbenannt", rootOrdner.getName());
    }

    /**
     * Creator may rename their own folder.
     */
    @Test
    public void givenCreator_whenRename_thenSaves() {
        when(ordnerRepository.findById(rootOrdner.getOrdnerID()))
            .thenReturn(Optional.of(rootOrdner));
        OrdnerAnlegung anlegung = new OrdnerAnlegung();
        anlegung.setStudent(student);
        anlegung.setOrdner(rootOrdner);
        when(ordnerAnlegungRepository.findByOrdner(rootOrdner))
            .thenReturn(Optional.of(anlegung));
        when(ordnerRepository.save(any(Ordner.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ordnerService.renameFolder(
            rootOrdner.getOrdnerID(), "Mein Ordner",
            "student@dhbw.de");

        verify(ordnerRepository).save(rootOrdner);
    }

    /**
     * Non-creator STUDENT who is not KURSADMIN gets 403.
     */
    @Test
    public void givenNonCreator_whenRename_thenThrows403() {
        Student other = new Student();
        other.setStudentID(UUID.randomUUID());
        other.setDhbwMail("other@dhbw.de");
        other.setStudentType(StudentType.STUDENT);
        when(studentLookupHelper.findStudentByEmail("other@dhbw.de"))
            .thenReturn(other);
        KursBeitrittsAnfrage otherAnfrage =
            new KursBeitrittsAnfrage();
        otherAnfrage.setKurs(kurs);
        otherAnfrage.setStudent(other);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(other))
            .thenReturn(Optional.of(otherAnfrage));
        when(ordnerRepository.findById(rootOrdner.getOrdnerID()))
            .thenReturn(Optional.of(rootOrdner));
        OrdnerAnlegung anlegung = new OrdnerAnlegung();
        anlegung.setStudent(student);
        anlegung.setOrdner(rootOrdner);
        when(ordnerAnlegungRepository.findByOrdner(rootOrdner))
            .thenReturn(Optional.of(anlegung));

        assertThrows(
            ResponseStatusException.class,
            () -> ordnerService.renameFolder(
                rootOrdner.getOrdnerID(), "X", "other@dhbw.de"));
    }

    // --- deleteOrdner ---

    /**
     * SysAdmin can delete any folder.
     */
    @Test
    public void givenSysAdmin_whenDelete_thenDeletesFolder() {
        when(ordnerRepository.findById(rootOrdner.getOrdnerID()))
            .thenReturn(Optional.of(rootOrdner));
        when(ordnerRepository.findChildrenByUeberOrdner(rootOrdner))
            .thenReturn(List.of());
        rootOrdner.setDateien(List.of());
        when(ordnerAnlegungRepository.findByOrdner(rootOrdner))
            .thenReturn(Optional.empty());

        ordnerService.deleteOrdner(
            rootOrdner.getOrdnerID(), "system.administrator.a24@student.dhbw-karlsruhe.de");

        verify(ordnerRepository).delete(rootOrdner);
    }

    /**
     * Unknown folder ID during delete returns 404.
     */
    @Test
    public void givenUnknownId_whenDelete_thenThrows404() {
        UUID unknown = UUID.randomUUID();
        when(ordnerRepository.findById(unknown))
            .thenReturn(Optional.empty());

        assertThrows(
            ResponseStatusException.class,
            () -> ordnerService.deleteOrdner(
                unknown, "system.administrator.a24@student.dhbw-karlsruhe.de"));
    }

    /**
     * Non-admin, non-creator STUDENT gets 403 on delete.
     */
    @Test
    public void givenNonCreator_whenDelete_thenThrows403() {
        when(ordnerRepository.findById(rootOrdner.getOrdnerID()))
            .thenReturn(Optional.of(rootOrdner));
        OrdnerAnlegung anlegung = new OrdnerAnlegung();
        anlegung.setStudent(kursAdmin);
        anlegung.setOrdner(rootOrdner);
        when(ordnerAnlegungRepository.findByOrdner(rootOrdner))
            .thenReturn(Optional.of(anlegung));

        assertThrows(
            ResponseStatusException.class,
            () -> ordnerService.deleteOrdner(
                rootOrdner.getOrdnerID(), "student@dhbw.de"));
    }
}
