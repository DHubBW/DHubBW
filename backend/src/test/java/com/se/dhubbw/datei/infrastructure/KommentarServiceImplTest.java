package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.application.dto.output.KommentarDTO;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.datei.infrastructure.db.Kommentar;
import com.se.dhubbw.datei.infrastructure.db.KommentarRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.shared.infrastructure.helper.StudentLookupHelper;
import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.infrastructure.db.Student;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link KommentarServiceImpl}.
 * Covers create, list, update and delete flows including
 * positive cases, validation errors, authorisation rejections, and
 * not-found edge cases.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class KommentarServiceImplTest {

    @Mock
    private KommentarRepository kommentarRepository;

    @Mock
    private DateiRepository dateiRepository;

    @Mock
    private StudentLookupHelper studentLookupHelper;

    @Mock
    private KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    @InjectMocks
    private KommentarServiceImpl kommentarService;

    private Student author;
    private Student otherStudent;
    private Student kursAdmin;
    private Student sysAdmin;
    private Kurs kurs;
    private Ordner publicOrdner;
    private Ordner privateOrdner;
    private Datei datei;
    private Kommentar kommentar;

    @BeforeEach
    public void setUp() {
        kurs = new Kurs();
        kurs.setKursID(UUID.randomUUID());

        author = new Student();
        author.setStudentID(UUID.randomUUID());
        author.setDhbwMail("autor@dhbw.de");
        author.setStudentType(StudentType.STUDENT);
        author.setName("Autor");

        otherStudent = new Student();
        otherStudent.setStudentID(UUID.randomUUID());
        otherStudent.setDhbwMail("other@dhbw.de");
        otherStudent.setStudentType(StudentType.STUDENT);
        otherStudent.setName("Other");

        kursAdmin = new Student();
        kursAdmin.setStudentID(UUID.randomUUID());
        kursAdmin.setDhbwMail("admin@dhbw.de");
        kursAdmin.setStudentType(StudentType.KURSADMIN);
        kursAdmin.setName("Admin");

        sysAdmin = new Student();
        sysAdmin.setStudentID(UUID.randomUUID());
        sysAdmin.setDhbwMail("sys@dhbw.de");
        sysAdmin.setStudentType(StudentType.SYSTEMADMIN);
        sysAdmin.setName("Sys");

        publicOrdner = new Ordner();
        publicOrdner.setOrdnerID(UUID.randomUUID());
        publicOrdner.setKurs(kurs);
        publicOrdner.setPublic(true);

        privateOrdner = new Ordner();
        privateOrdner.setOrdnerID(UUID.randomUUID());
        privateOrdner.setKurs(kurs);
        privateOrdner.setPublic(false);

        datei = new Datei();
        datei.setDateiID(UUID.randomUUID());
        datei.setOrdner(publicOrdner);
        datei.setName("test.pdf");

        kommentar = new Kommentar();
        kommentar.setKommentarID(UUID.randomUUID());
        kommentar.setDatei(datei);
        kommentar.setAutor(author);
        kommentar.setInhalt("Initialer Inhalt");
        kommentar.setBearbeitet(false);
    }

    private KursBeitrittsAnfrage approvedEnrollment(
            final Student student) {
        KursBeitrittsAnfrage anfrage = new KursBeitrittsAnfrage();
        anfrage.setKurs(kurs);
        anfrage.setStudent(student);
        anfrage.setApproved(true);
        return anfrage;
    }

    // --- create ---

    @Test
    public void givenCourseMember_whenCreate_thenSavesAndReturns() {
        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(author);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(author))
            .thenReturn(true);
        when(kommentarRepository.save(any(Kommentar.class)))
            .thenAnswer(inv -> {
                Kommentar k = inv.getArgument(0);
                k.setKommentarID(UUID.randomUUID());
                return k;
            });

        KommentarDTO dto = kommentarService.createKommentar(
            datei.getDateiID(), "Mein Kommentar", "autor@dhbw.de");

        assertNotNull(dto);
        assertEquals("Mein Kommentar", dto.inhalt());
        assertEquals(author.getStudentID(), dto.autorID());
        assertNotNull(dto.erstelltAm());
        assertNull(dto.zuletztBearbeitet());
        assertTrue(dto.canEdit());
        assertTrue(dto.canDelete());
        verify(kommentarRepository).save(any(Kommentar.class));
    }

    @Test
    public void givenBlankInhalt_whenCreate_thenThrowsBadRequest() {
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> kommentarService.createKommentar(
                datei.getDateiID(), "   ", "autor@dhbw.de"));
        assertEquals(400, ex.getStatusCode().value());
        verify(kommentarRepository, never())
            .save(any(Kommentar.class));
    }

    @Test
    public void givenUnknownDatei_whenCreate_thenThrowsNotFound() {
        when(dateiRepository.findById(any(UUID.class)))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> kommentarService.createKommentar(
                UUID.randomUUID(), "Hi", "autor@dhbw.de"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenUnenrolledStudent_whenCreate_thenForbidden() {
        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(author);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(author))
            .thenReturn(false);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> kommentarService.createKommentar(
                datei.getDateiID(), "Hi", "autor@dhbw.de"));
        assertEquals(403, ex.getStatusCode().value());
    }

    // --- list ---

    @Test
    public void givenAuthor_whenList_thenReturnsCommentsWithFlags() {
        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(author);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(author))
            .thenReturn(true);
        when(kommentarRepository
            .findByDateiIdOrderByErstelltAmAsc(datei.getDateiID()))
            .thenReturn(List.of(kommentar));

        List<KommentarDTO> result = kommentarService
            .retrieveKommentareForDatei(
                datei.getDateiID(), "autor@dhbw.de");

        assertEquals(1, result.size());
        assertTrue(result.get(0).canEdit());
        assertTrue(result.get(0).canDelete());
    }

    @Test
    public void givenOtherStudent_whenList_thenCanEditFalse() {
        when(studentLookupHelper.findStudentByEmail("other@dhbw.de"))
            .thenReturn(otherStudent);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(otherStudent))
            .thenReturn(true);
        when(kommentarRepository
            .findByDateiIdOrderByErstelltAmAsc(datei.getDateiID()))
            .thenReturn(List.of(kommentar));

        List<KommentarDTO> result = kommentarService
            .retrieveKommentareForDatei(
                datei.getDateiID(), "other@dhbw.de");

        assertEquals(1, result.size());
        // Other student can see but cannot edit/delete somebody else's
        // comment.
        assertEquals(false, result.get(0).canEdit());
        assertEquals(false, result.get(0).canDelete());
    }

    @Test
    public void givenPrivateOrdnerNonMember_whenList_thenForbidden() {
        datei.setOrdner(privateOrdner);
        when(studentLookupHelper.findStudentByEmail("other@dhbw.de"))
            .thenReturn(otherStudent);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(otherStudent))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> kommentarService.retrieveKommentareForDatei(
                datei.getDateiID(), "other@dhbw.de"));
        assertEquals(403, ex.getStatusCode().value());
    }

    // --- update ---

    @Test
    public void givenAuthor_whenUpdate_thenSetsBearbeitetFlag() {
        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(author);
        when(kommentarRepository.findById(kommentar.getKommentarID()))
            .thenReturn(Optional.of(kommentar));
        when(kommentarRepository.save(any(Kommentar.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        KommentarDTO dto = kommentarService.updateKommentar(
            kommentar.getKommentarID(),
            "Neuer Inhalt",
            "autor@dhbw.de");

        assertEquals("Neuer Inhalt", dto.inhalt());
        assertEquals(true, dto.bearbeitet());
        assertNotNull(dto.zuletztBearbeitet());
    }

    @Test
    public void givenNonAuthor_whenUpdate_thenForbidden() {
        when(studentLookupHelper.findStudentByEmail("other@dhbw.de"))
            .thenReturn(otherStudent);
        when(kommentarRepository.findById(kommentar.getKommentarID()))
            .thenReturn(Optional.of(kommentar));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> kommentarService.updateKommentar(
                kommentar.getKommentarID(),
                "Neuer Inhalt",
                "other@dhbw.de"));
        assertEquals(403, ex.getStatusCode().value());
        verify(kommentarRepository, never())
            .save(any(Kommentar.class));
    }

    @Test
    public void givenSysAdmin_whenUpdate_thenStillForbidden() {
        // Update is author-only; sysadmins must NOT be able to edit
        // someone else's wording.
        when(studentLookupHelper.findStudentByEmail("sys@dhbw.de"))
            .thenReturn(sysAdmin);
        when(kommentarRepository.findById(kommentar.getKommentarID()))
            .thenReturn(Optional.of(kommentar));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> kommentarService.updateKommentar(
                kommentar.getKommentarID(),
                "Neuer Inhalt",
                "sys@dhbw.de"));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    public void givenUnknownKommentar_whenUpdate_thenNotFound() {
        when(kommentarRepository.findById(any(UUID.class)))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> kommentarService.updateKommentar(
                UUID.randomUUID(), "Neu", "autor@dhbw.de"));
        assertEquals(404, ex.getStatusCode().value());
    }

    // --- delete ---

    @Test
    public void givenAuthor_whenDelete_thenDeletes() {
        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(author);
        when(kommentarRepository.findById(kommentar.getKommentarID()))
            .thenReturn(Optional.of(kommentar));

        kommentarService.deleteKommentar(
            kommentar.getKommentarID(), "autor@dhbw.de");

        verify(kommentarRepository).delete(kommentar);
    }

    @Test
    public void givenSysAdmin_whenDelete_thenDeletes() {
        when(studentLookupHelper.findStudentByEmail("sys@dhbw.de"))
            .thenReturn(sysAdmin);
        when(kommentarRepository.findById(kommentar.getKommentarID()))
            .thenReturn(Optional.of(kommentar));

        kommentarService.deleteKommentar(
            kommentar.getKommentarID(), "sys@dhbw.de");

        verify(kommentarRepository).delete(kommentar);
    }

    @Test
    public void givenKursAdminOfFolder_whenDelete_thenDeletes() {
        when(studentLookupHelper.findStudentByEmail("admin@dhbw.de"))
            .thenReturn(kursAdmin);
        when(kommentarRepository.findById(kommentar.getKommentarID()))
            .thenReturn(Optional.of(kommentar));
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(kursAdmin))
            .thenReturn(Optional.of(approvedEnrollment(kursAdmin)));

        kommentarService.deleteKommentar(
            kommentar.getKommentarID(), "admin@dhbw.de");

        verify(kommentarRepository).delete(kommentar);
    }

    @Test
    public void givenStrangerStudent_whenDelete_thenForbidden() {
        when(studentLookupHelper.findStudentByEmail("other@dhbw.de"))
            .thenReturn(otherStudent);
        when(kommentarRepository.findById(kommentar.getKommentarID()))
            .thenReturn(Optional.of(kommentar));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> kommentarService.deleteKommentar(
                kommentar.getKommentarID(), "other@dhbw.de"));
        assertEquals(403, ex.getStatusCode().value());
        verify(kommentarRepository, never()).delete(any());
    }

    @Test
    public void givenUnknownKommentar_whenDelete_thenNotFound() {
        when(kommentarRepository.findById(any(UUID.class)))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> kommentarService.deleteKommentar(
                UUID.randomUUID(), "autor@dhbw.de"));
        assertEquals(404, ex.getStatusCode().value());
    }
}
