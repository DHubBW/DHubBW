package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.application.dto.KursanfrageDTO;
import com.se.dhubbw.kurs.infrastructure.KursAdminServiceImpl;
import com.se.dhubbw.kurs.infrastructure.KursStudentHelper;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.infrastructure.db.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class KursAdminServiceTest {

    @Mock
    private KursBeitrittsAnfrageRepository kursBeitrittsAnfrageRepository;

    @Mock
    private KursStudentHelper kursStudentHelper;

    @InjectMocks
    private KursAdminServiceImpl kursAdminService;

    private Kurs kurs;
    private KursBeitrittsAnfrage pendingAnfrage;

    @BeforeEach
    public void setUp() {
        kurs = new Kurs();
        kurs.setKursID(UUID.randomUUID());
        kurs.setKursName("TINF24B1");

        Student pendingStudent = new Student();
        pendingStudent.setStudentID(UUID.randomUUID());
        pendingStudent.setName("Lena");

        pendingAnfrage = new KursBeitrittsAnfrage();
        pendingAnfrage.setAnfrageID(UUID.randomUUID());
        pendingAnfrage.setKurs(kurs);
        pendingAnfrage.setApproved(false);
        pendingAnfrage.setStudent(pendingStudent);
    }

    @Test
    public void givenAdminWithCourse_whenRetrieve_thenReturnsPending() {
        when(kursStudentHelper.findKursForAdminByEmail("admin@dhbw.de"))
            .thenReturn(kurs);
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedFalse(kurs))
            .thenReturn(List.of(pendingAnfrage));

        List<KursanfrageDTO> result =
            kursAdminService.retrieveKursbeitrittsanfragen("admin@dhbw.de");

        assertEquals(1, result.size());
        assertEquals(
            pendingAnfrage.getAnfrageID(), result.getFirst().getAnfrageID());
    }

    @Test
    public void givenUnknownAdmin_whenRetrieve_thenThrowsException() {
        when(kursStudentHelper.findKursForAdminByEmail("unknown@dhbw.de"))
            .thenThrow(new NoSuchElementException("Student not found"));

        assertThrows(NoSuchElementException.class,
            () -> kursAdminService
                .retrieveKursbeitrittsanfragen("unknown@dhbw.de"));
    }

    @Test
    public void givenAdminNoCourse_whenRetrieve_thenThrowsException() {
        when(kursStudentHelper.findKursForAdminByEmail("admin@dhbw.de"))
            .thenThrow(new NoSuchElementException("No course assignment"));

        assertThrows(NoSuchElementException.class,
            () -> kursAdminService
                .retrieveKursbeitrittsanfragen("admin@dhbw.de"));
    }

    @Test
    public void givenValidRequest_whenApprove_thenSetsApproved() {
        UUID id = pendingAnfrage.getAnfrageID();
        when(kursStudentHelper.findKursForAdminByEmail("admin@dhbw.de"))
            .thenReturn(kurs);
        when(kursBeitrittsAnfrageRepository.findById(id))
            .thenReturn(Optional.of(pendingAnfrage));

        kursAdminService.approveKursbeitrittsanfrage(id, "admin@dhbw.de");

        assertTrue(pendingAnfrage.isApproved());
        verify(kursBeitrittsAnfrageRepository).save(pendingAnfrage);
    }

    @Test
    public void givenUnknownId_whenApprove_thenThrowsException() {
        UUID unknownId = UUID.randomUUID();
        when(kursStudentHelper.findKursForAdminByEmail("admin@dhbw.de"))
            .thenReturn(kurs);
        when(kursBeitrittsAnfrageRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> kursAdminService
                .approveKursbeitrittsanfrage(unknownId, "admin@dhbw.de"));
    }

    @Test
    public void givenOtherCourseRequest_whenApprove_thenThrowsException() {
        Kurs otherKurs = new Kurs();
        otherKurs.setKursID(UUID.randomUUID());
        KursBeitrittsAnfrage otherAnfrage = new KursBeitrittsAnfrage();
        otherAnfrage.setAnfrageID(UUID.randomUUID());
        otherAnfrage.setKurs(otherKurs);

        UUID id = otherAnfrage.getAnfrageID();
        when(kursStudentHelper.findKursForAdminByEmail("admin@dhbw.de"))
            .thenReturn(kurs);
        when(kursBeitrittsAnfrageRepository.findById(id))
            .thenReturn(Optional.of(otherAnfrage));

        assertThrows(IllegalArgumentException.class,
            () -> kursAdminService
                .approveKursbeitrittsanfrage(id, "admin@dhbw.de"));
    }

    @Test
    public void givenAlreadyEnrolledStudent_whenApprove_thenThrowsException() {
        Student enrolledStudent = new Student();

        KursBeitrittsAnfrage requestForEnrolled =
            new KursBeitrittsAnfrage();
        requestForEnrolled.setAnfrageID(UUID.randomUUID());
        requestForEnrolled.setKurs(kurs);
        requestForEnrolled.setStudent(enrolledStudent);

        UUID id = requestForEnrolled.getAnfrageID();
        when(kursStudentHelper.findKursForAdminByEmail("admin@dhbw.de"))
            .thenReturn(kurs);
        when(kursBeitrittsAnfrageRepository.findById(id))
            .thenReturn(Optional.of(requestForEnrolled));
        when(kursBeitrittsAnfrageRepository
            .existsByStudentAndIsApprovedTrue(enrolledStudent))
            .thenReturn(true);

        assertThrows(IllegalStateException.class,
            () -> kursAdminService
                .approveKursbeitrittsanfrage(id, "admin@dhbw.de"));
    }

    @Test
    public void givenValidRequest_whenDecline_thenDeletesRequest() {
        UUID id = pendingAnfrage.getAnfrageID();
        when(kursStudentHelper.findKursForAdminByEmail("admin@dhbw.de"))
            .thenReturn(kurs);
        when(kursBeitrittsAnfrageRepository.findById(id))
            .thenReturn(Optional.of(pendingAnfrage));

        kursAdminService.declineKursbeitrittsanfrage(id, "admin@dhbw.de");

        verify(kursBeitrittsAnfrageRepository).delete(pendingAnfrage);
    }

    @Test
    public void givenUnknownId_whenDecline_thenThrowsException() {
        UUID unknownId = UUID.randomUUID();
        when(kursStudentHelper.findKursForAdminByEmail("admin@dhbw.de"))
            .thenReturn(kurs);
        when(kursBeitrittsAnfrageRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> kursAdminService
                .declineKursbeitrittsanfrage(unknownId, "admin@dhbw.de"));
    }

    @Test
    public void givenOtherCourseRequest_whenDecline_thenThrowsException() {
        Kurs otherKurs = new Kurs();
        otherKurs.setKursID(UUID.randomUUID());
        KursBeitrittsAnfrage otherAnfrage = new KursBeitrittsAnfrage();
        otherAnfrage.setAnfrageID(UUID.randomUUID());
        otherAnfrage.setKurs(otherKurs);

        UUID id = otherAnfrage.getAnfrageID();
        when(kursStudentHelper.findKursForAdminByEmail("admin@dhbw.de"))
            .thenReturn(kurs);
        when(kursBeitrittsAnfrageRepository.findById(id))
            .thenReturn(Optional.of(otherAnfrage));

        assertThrows(IllegalArgumentException.class,
            () -> kursAdminService
                .declineKursbeitrittsanfrage(id, "admin@dhbw.de"));
    }
}
