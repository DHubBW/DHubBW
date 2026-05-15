package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursRepository;
import com.se.dhubbw.kurs.infrastructure.KursSystemAdminServiceImpl;
import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class KursSystemAdminServiceTest {

    @Mock
    private KursErstellungsAnfrageRepository erstellungsAnfrageRepository;

    @Mock
    private KursBeitrittsAnfrageRepository kursBeitrittsAnfrageRepository;

    @Mock
    private KursRepository kursRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private KursSystemAdminServiceImpl kursSystemAdminService;

    private Student student;
    private KursErstellungsAnfrage anfrage;

    @BeforeEach
    public void setUp() {
        student = new Student();
        student.setStudentType(StudentType.STUDENT);

        anfrage = new KursErstellungsAnfrage();
        anfrage.setAnfrageID(UUID.randomUUID());
        anfrage.setStudent(student);
        anfrage.setKursName("TINF26A1");
        anfrage.setJahrgang("2026");
        anfrage.setApproved(false);
    }

    @Test
    public void givenPendingRequests_whenRetrieve_thenReturnsList() {
        when(erstellungsAnfrageRepository.findByIsApprovedFalse())
            .thenReturn(List.of(anfrage));

        List<KursErstellungsAnfrage> result =
            kursSystemAdminService.retrieveKursErstellungsanfragen();

        assertEquals(1, result.size());
        assertEquals(anfrage, result.getFirst());
    }

    @Test
    public void givenValidRequest_whenApprove_thenCreatesKursAndSetsAdmin() {
        UUID id = anfrage.getAnfrageID();
        when(erstellungsAnfrageRepository.findById(id))
            .thenReturn(Optional.of(anfrage));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(student))
            .thenReturn(false);
        when(kursRepository.existsByKursNameAndJahrgang(
            "TINF26A1", "2026"))
            .thenReturn(false);

        kursSystemAdminService.approveKursErstellungsanfrage(id);

        verify(kursRepository).save(any(Kurs.class));
        assertEquals(StudentType.KURSADMIN, student.getStudentType());
        verify(studentRepository).save(student);
        verify(kursBeitrittsAnfrageRepository)
            .save(any(KursBeitrittsAnfrage.class));
        assertTrue(anfrage.isApproved());
        verify(erstellungsAnfrageRepository).save(anfrage);
    }

    @Test
    public void givenApprove_whenCreatesKurs_thenKursHasCorrectData() {
        UUID id = anfrage.getAnfrageID();
        when(erstellungsAnfrageRepository.findById(id))
            .thenReturn(Optional.of(anfrage));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(student))
            .thenReturn(false);
        when(kursRepository.existsByKursNameAndJahrgang(
            "TINF26A1", "2026"))
            .thenReturn(false);

        kursSystemAdminService.approveKursErstellungsanfrage(id);

        ArgumentCaptor<Kurs> kursCaptor =
            ArgumentCaptor.forClass(Kurs.class);
        verify(kursRepository).save(kursCaptor.capture());
        assertEquals("TINF26A1", kursCaptor.getValue().getKursName());
        assertEquals("2026", kursCaptor.getValue().getJahrgang());
    }

    @Test
    public void givenUnknownId_whenApprove_thenThrowsException() {
        UUID unknownId = UUID.randomUUID();
        when(erstellungsAnfrageRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> kursSystemAdminService
                .approveKursErstellungsanfrage(unknownId));
        verify(kursRepository, never()).save(any());
    }

    @Test
    public void givenEnrolledStudent_whenApprove_thenThrowsException() {
        UUID id = anfrage.getAnfrageID();
        when(erstellungsAnfrageRepository.findById(id))
            .thenReturn(Optional.of(anfrage));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(student))
            .thenReturn(true);

        assertThrows(IllegalStateException.class,
            () -> kursSystemAdminService
                .approveKursErstellungsanfrage(id));
        verify(kursRepository, never()).save(any());
    }

    @Test
    public void givenExistingCourse_whenApprove_thenThrowsException() {
        UUID id = anfrage.getAnfrageID();
        when(erstellungsAnfrageRepository.findById(id))
            .thenReturn(Optional.of(anfrage));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(student))
            .thenReturn(false);
        when(kursRepository.existsByKursNameAndJahrgang(
            "TINF26A1", "2026"))
            .thenReturn(true);

        assertThrows(IllegalArgumentException.class,
            () -> kursSystemAdminService
                .approveKursErstellungsanfrage(id));
        verify(studentRepository, never()).save(any());
    }

    @Test
    public void givenValidRequest_whenDecline_thenDeletesAnfrage() {
        UUID id = anfrage.getAnfrageID();
        when(erstellungsAnfrageRepository.findById(id))
            .thenReturn(Optional.of(anfrage));

        kursSystemAdminService.declineKursErstellungsanfrage(id);

        verify(erstellungsAnfrageRepository).delete(anfrage);
    }

    @Test
    public void givenUnknownId_whenDecline_thenThrowsException() {
        UUID unknownId = UUID.randomUUID();
        when(erstellungsAnfrageRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> kursSystemAdminService
                .declineKursErstellungsanfrage(unknownId));
        verify(erstellungsAnfrageRepository, never()).delete(any());
    }
}
