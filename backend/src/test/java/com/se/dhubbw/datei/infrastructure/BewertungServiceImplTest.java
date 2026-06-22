package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.application.dto.output.DateiBewertungDTO;
import com.se.dhubbw.datei.infrastructure.db.Bewertung;
import com.se.dhubbw.datei.infrastructure.db.BewertungRepository;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BewertungServiceImpl}.
 * Covers create / update / remove flows and aggregate consistency.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BewertungServiceImplTest {

    @Mock
    private BewertungRepository bewertungRepository;

    @Mock
    private DateiRepository dateiRepository;

    @Mock
    private StudentLookupHelper studentLookupHelper;

    @Mock
    private KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    @InjectMocks
    private BewertungServiceImpl bewertungService;

    private Student autor;
    private Kurs kurs;
    private Ordner ordner;
    private Datei datei;

    @BeforeEach
    public void setUp() {
        kurs = new Kurs();
        kurs.setKursID(UUID.randomUUID());

        autor = new Student();
        autor.setStudentID(UUID.randomUUID());
        autor.setDhbwMail("autor@dhbw.de");
        autor.setStudentType(StudentType.STUDENT);

        ordner = new Ordner();
        ordner.setOrdnerID(UUID.randomUUID());
        ordner.setKurs(kurs);
        ordner.setPublic(true);

        datei = new Datei();
        datei.setDateiID(UUID.randomUUID());
        datei.setOrdner(ordner);
        datei.setBewertungSumme(0L);
        datei.setBewertungAnzahl(0);
    }

    @Test
    public void givenNewRating_whenSet_thenAggregateIncrements() {
        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(autor);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(autor))
            .thenReturn(true);
        when(bewertungRepository
            .findByDateiAndAutor(
                datei.getDateiID(), autor.getStudentID()))
            .thenReturn(Optional.empty());

        DateiBewertungDTO result = bewertungService.setBewertung(
            datei.getDateiID(), 4, "autor@dhbw.de");

        assertEquals(4.0, result.durchschnitt(), 0.0001);
        assertEquals(1, result.anzahl());
        assertEquals(4, result.eigeneSterne());
        verify(bewertungRepository).save(any(Bewertung.class));
        verify(dateiRepository).save(datei);
    }

    @Test
    public void givenExistingRating_whenSet_thenSummeIsAdjusted() {
        Bewertung existing = new Bewertung();
        existing.setBewertungID(UUID.randomUUID());
        existing.setDatei(datei);
        existing.setAutor(autor);
        existing.setSterne(2);
        datei.setBewertungSumme(2L);
        datei.setBewertungAnzahl(1);

        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(autor);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(autor))
            .thenReturn(true);
        when(bewertungRepository
            .findByDateiAndAutor(
                datei.getDateiID(), autor.getStudentID()))
            .thenReturn(Optional.of(existing));

        DateiBewertungDTO result = bewertungService.setBewertung(
            datei.getDateiID(), 5, "autor@dhbw.de");

        assertEquals(5.0, result.durchschnitt(), 0.0001);
        assertEquals(1, result.anzahl());
        assertEquals(5, result.eigeneSterne());
        // Count stays at 1; sum becomes 5 (was 2 + delta of +3).
        assertEquals(5L, datei.getBewertungSumme());
        assertEquals(1, datei.getBewertungAnzahl());
    }

    @Test
    public void givenStarsOutOfRange_whenSet_thenBadRequest() {
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> bewertungService.setBewertung(
                datei.getDateiID(), 6, "autor@dhbw.de"));
        assertEquals(400, ex.getStatusCode().value());
        verify(bewertungRepository, never())
            .save(any(Bewertung.class));
    }

    @Test
    public void givenExistingRating_whenRemove_thenAggregateDecrements() {
        Bewertung existing = new Bewertung();
        existing.setBewertungID(UUID.randomUUID());
        existing.setDatei(datei);
        existing.setAutor(autor);
        existing.setSterne(3);
        datei.setBewertungSumme(3L);
        datei.setBewertungAnzahl(1);

        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(autor);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(bewertungRepository
            .findByDateiAndAutor(
                datei.getDateiID(), autor.getStudentID()))
            .thenReturn(Optional.of(existing));

        DateiBewertungDTO result = bewertungService.removeBewertung(
            datei.getDateiID(), "autor@dhbw.de");

        assertEquals(0.0, result.durchschnitt(), 0.0001);
        assertEquals(0, result.anzahl());
        assertEquals(0, result.eigeneSterne());
        verify(bewertungRepository).delete(existing);
        assertEquals(0L, datei.getBewertungSumme());
        assertEquals(0, datei.getBewertungAnzahl());
    }

    @Test
    public void givenNoRating_whenRemove_thenNotFound() {
        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(autor);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(bewertungRepository
            .findByDateiAndAutor(
                datei.getDateiID(), autor.getStudentID()))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> bewertungService.removeBewertung(
                datei.getDateiID(), "autor@dhbw.de"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void givenUnenrolled_whenSet_thenForbidden() {
        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(autor);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(autor))
            .thenReturn(false);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> bewertungService.setBewertung(
                datei.getDateiID(), 3, "autor@dhbw.de"));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    public void givenSysAdmin_whenSet_thenSucceedsRegardless() {
        autor.setStudentType(StudentType.SYSTEMADMIN);
        when(studentLookupHelper.findStudentByEmail("autor@dhbw.de"))
            .thenReturn(autor);
        when(dateiRepository.findById(datei.getDateiID()))
            .thenReturn(Optional.of(datei));
        when(bewertungRepository
            .findByDateiAndAutor(
                datei.getDateiID(), autor.getStudentID()))
            .thenReturn(Optional.empty());

        DateiBewertungDTO result = bewertungService.setBewertung(
            datei.getDateiID(), 5, "autor@dhbw.de");

        assertTrue(result.eigeneSterne() == 5);
    }
}
