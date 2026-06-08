package com.se.dhubbw.dashboard.application;

import com.se.dhubbw.dashboard.infrastructure.DashboardServiceImpl;
import com.se.dhubbw.datei.application.dto.output.DateiDTO;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.mensa.domain.MensaService;
import com.se.dhubbw.mensa.infrastructure.db.Mensa;
import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class DashboardServiceImplTest {

    @Mock
    private MensaService mensaService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private KursBeitrittsAnfrageRepository kursBeitrittsAnfrageRepository;

    @Mock
    private DateiRepository dateiRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardServiceImpl;

    @Test
    public void givenMensaService_whenGetMealsForToday_thenDelegatesToMensaService() {
        List<Mensa> expected = List.of(new Mensa());
        when(mensaService.getMealsForDay(LocalDate.now())).thenReturn(expected);

        List<Mensa> result = dashboardServiceImpl.getMealsForToday();

        assertEquals(expected, result);
        verify(mensaService).getMealsForDay(LocalDate.now());
    }

    @Test
    public void givenMoreThanFiveDateien_whenGetAllLatestDateien_thenLimitedToFive() {
        List<Datei> allDateien = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            allDateien.add(buildDatei());
        }
        when(dateiRepository.findAllOrderByZuletztBearbeitetDesc())
            .thenReturn(allDateien);

        List<DateiDTO> result = dashboardServiceImpl.getAllLatestDateien();

        assertEquals(5, result.size());
    }

    @Test
    public void givenFewDateien_whenGetAllLatestDateien_thenReturnsAll() {
        List<Datei> allDateien = List.of(buildDatei(), buildDatei());
        when(dateiRepository.findAllOrderByZuletztBearbeitetDesc())
            .thenReturn(allDateien);

        List<DateiDTO> result = dashboardServiceImpl.getAllLatestDateien();

        assertEquals(2, result.size());
    }

    @Test
    public void givenUnknownStudent_whenGetLatestDateienForStudent_thenReturnsEmpty() {
        when(studentRepository.findByDhbwMail("unknown@dhbw.de"))
            .thenReturn(Optional.empty());

        Optional<List<DateiDTO>> result =
            dashboardServiceImpl.getLatestDateienForStudent("unknown@dhbw.de");

        assertTrue(result.isEmpty());
    }

    @Test
    public void givenStudentWithNoApprovedEnrollment_whenGetLatestDateienForStudent_thenReturnsEmpty() {
        Student student = new Student();
        when(studentRepository.findByDhbwMail("unapproved@dhbw.de"))
            .thenReturn(Optional.of(student));
        when(kursBeitrittsAnfrageRepository.findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.empty());

        Optional<List<DateiDTO>> result =
            dashboardServiceImpl.getLatestDateienForStudent("unapproved@dhbw.de");

        assertTrue(result.isEmpty());
    }

    @Test
    public void givenStudentWithApprovedEnrollment_whenGetLatestDateienForStudent_thenReturnsDateien() {
        Student student = new Student();
        UUID kursId = UUID.randomUUID();
        Kurs kurs = new Kurs();
        kurs.setKursID(kursId);
        KursBeitrittsAnfrage anfrage = new KursBeitrittsAnfrage();
        anfrage.setKurs(kurs);
        List<Datei> dateien = List.of(buildDatei());
        when(studentRepository.findByDhbwMail("approved@dhbw.de"))
            .thenReturn(Optional.of(student));
        when(kursBeitrittsAnfrageRepository.findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(anfrage));
        when(dateiRepository.findByKursIdOrderByZuletztBearbeitetDesc(kursId))
            .thenReturn(dateien);

        Optional<List<DateiDTO>> result =
            dashboardServiceImpl.getLatestDateienForStudent("approved@dhbw.de");

        assertTrue(result.isPresent());
        assertEquals(1, result.get().size());
    }

    @Test
    public void givenMoreThanFiveDateienForStudent_whenGetLatestDateienForStudent_thenLimitedToFive() {
        Student student = new Student();
        UUID kursId = UUID.randomUUID();
        Kurs kurs = new Kurs();
        kurs.setKursID(kursId);
        KursBeitrittsAnfrage anfrage = new KursBeitrittsAnfrage();
        anfrage.setKurs(kurs);
        List<Datei> allDateien = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            allDateien.add(buildDatei());
        }
        when(studentRepository.findByDhbwMail("approved@dhbw.de"))
            .thenReturn(Optional.of(student));
        when(kursBeitrittsAnfrageRepository.findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(anfrage));
        when(dateiRepository.findByKursIdOrderByZuletztBearbeitetDesc(kursId))
            .thenReturn(allDateien);

        Optional<List<DateiDTO>> result =
            dashboardServiceImpl.getLatestDateienForStudent("approved@dhbw.de");

        assertTrue(result.isPresent());
        assertEquals(5, result.get().size());
    }

    /**
     * Build a minimal {@link Datei} with enough state for {@code toDateiDto}
     * to succeed: a non-null {@code ordner} with ID and name set.
     */
    private static Datei buildDatei() {
        Ordner ordner = new Ordner();
        ordner.setOrdnerID(UUID.randomUUID());
        ordner.setName("Test-Ordner");
        Datei datei = new Datei();
        datei.setOrdner(ordner);
        datei.setName("test.pdf");
        datei.setZuletztBearbeitet(LocalDate.now());
        datei.setFileSize(1.0f);
        datei.setDownloadAnzahl(0);
        return datei;
    }
}
