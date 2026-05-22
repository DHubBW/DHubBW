package com.se.dhubbw.student.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrageRepository;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerAnlegungRepository;
import com.se.dhubbw.shared.infrastructure.helper.StudentLookupHelper;
import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.application.dto.StudentProfileDTO;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StudentDashboardServiceImpl}.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
public class StudentDashboardServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentLookupHelper studentLookupHelper;

    @Mock
    private KursBeitrittsAnfrageRepository kursBeitrittsAnfrageRepository;

    @Mock
    private KursErstellungsAnfrageRepository kursErstellungsAnfrageRepository;

    @Mock
    private DateiAnlegungRepository dateiAnlegungRepository;

    @Mock
    private OrdnerAnlegungRepository ordnerAnlegungRepository;

    @Mock
    private DateiRepository dateiRepository;

    @InjectMocks
    private StudentDashboardServiceImpl studentDashboardService;

    private Student student;
    private Student otherStudent;
    private Kurs kurs;
    private KursBeitrittsAnfrage enrollment;
    private KursBeitrittsAnfrage otherEnrollment;

    private static final String STUDENT_MAIL = "max.mustermann.12345@student.dhbw-karlsruhe.de";
    private static final String OTHER_MAIL = "lisa.mueller.54321@student.dhbw-karlsruhe.de";

    @BeforeEach
    public void setUp() {
        student = new Student();
        student.setStudentID(UUID.randomUUID());
        student.setDhbwMail(STUDENT_MAIL);
        student.setName("Max Mustermann");
        student.setStudentType(StudentType.STUDENT);

        otherStudent = new Student();
        otherStudent.setStudentID(UUID.randomUUID());
        otherStudent.setDhbwMail(OTHER_MAIL);
        otherStudent.setName("Lisa Mueller");
        otherStudent.setStudentType(StudentType.KURSADMIN);

        kurs = new Kurs();
        kurs.setKursID(UUID.randomUUID());
        kurs.setKursName("TINF24B2");
        kurs.setJahrgang("2024");

        enrollment = new KursBeitrittsAnfrage();
        enrollment.setAnfrageID(UUID.randomUUID());
        enrollment.setKurs(kurs);
        enrollment.setStudent(student);
        enrollment.setApproved(true);

        otherEnrollment = new KursBeitrittsAnfrage();
        otherEnrollment.setAnfrageID(UUID.randomUUID());
        otherEnrollment.setKurs(kurs);
        otherEnrollment.setStudent(otherStudent);
        otherEnrollment.setApproved(true);
    }

    // ==================== leaveKurs Tests ====================

    @Test
    public void givenRegularStudent_whenLeaveKurs_thenSuccessfullyLeaves() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));

        studentDashboardService.leaveKurs(STUDENT_MAIL);

        verify(kursBeitrittsAnfrageRepository).delete(enrollment);
    }

    @Test
    public void givenStudentNotFound_whenLeaveKurs_thenThrowsException() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenThrow(new NoSuchElementException("Student not found: " + STUDENT_MAIL));

        NoSuchElementException exception = assertThrows(
            NoSuchElementException.class,
            () -> studentDashboardService.leaveKurs(STUDENT_MAIL)
        );

        assertEquals("Student not found: " + STUDENT_MAIL, exception.getMessage());
        verify(kursBeitrittsAnfrageRepository, never()).delete(any());
    }

    @Test
    public void givenStudentNoEnrollment_whenLeaveKurs_thenThrowsException() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
            NoSuchElementException.class,
            () -> studentDashboardService.leaveKurs(STUDENT_MAIL)
        );

        assertEquals("Student has no course enrollment: " + STUDENT_MAIL,
            exception.getMessage());
        verify(kursBeitrittsAnfrageRepository, never()).delete(any());
    }

    @Test
    public void givenKursadminWithOthers_whenLeaveKurs_thenSuccessfullyLeaves() {
        student.setStudentType(StudentType.KURSADMIN);
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(enrollment, otherEnrollment));

        studentDashboardService.leaveKurs(STUDENT_MAIL);

        verify(kursBeitrittsAnfrageRepository).delete(enrollment);
    }

    @Test
    public void givenLastKursadmin_whenLeaveKurs_thenThrowsException() {
        student.setStudentType(StudentType.KURSADMIN);
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(enrollment));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> studentDashboardService.leaveKurs(STUDENT_MAIL)
        );

        assertEquals("Cannot leave course or delete account: "
            + "You are the last KURSADMIN in this course",
            exception.getMessage());
        verify(kursBeitrittsAnfrageRepository, never()).delete(any());
    }

    // ==================== deleteAccount Tests ====================

    @Test
    public void givenRegularStudent_whenDeleteAccount_thenSuccessfullyDeletes() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));
        when(kursBeitrittsAnfrageRepository
            .findAllByStudentAndIsApprovedFalse(student))
            .thenReturn(new ArrayList<>());

        studentDashboardService.deleteAccount(STUDENT_MAIL);

        verify(dateiAnlegungRepository).deleteByStudent(student);
        verify(ordnerAnlegungRepository).deleteByStudent(student);
        verify(dateiRepository).setUploaderToNullByUploader(student);
        verify(kursErstellungsAnfrageRepository).deleteByStudent(student);
        verify(kursBeitrittsAnfrageRepository).delete(enrollment);
        verify(studentRepository).delete(student);
    }

    @Test
    public void givenStudentNotFound_whenDeleteAccount_thenThrowsException() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenThrow(new NoSuchElementException("Student not found: " + STUDENT_MAIL));

        NoSuchElementException exception = assertThrows(
            NoSuchElementException.class,
            () -> studentDashboardService.deleteAccount(STUDENT_MAIL)
        );

        assertEquals("Student not found: " + STUDENT_MAIL, exception.getMessage());
        verify(studentRepository, never()).delete(any());
    }

    @Test
    public void givenLastSystemadmin_whenDeleteAccount_thenThrowsException() {
        student.setStudentType(StudentType.SYSTEMADMIN);
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(studentRepository.countByStudentType(StudentType.SYSTEMADMIN))
            .thenReturn(1L);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> studentDashboardService.deleteAccount(STUDENT_MAIL)
        );

        assertEquals("Cannot delete account: You are the last SYSTEMADMIN",
            exception.getMessage());
        verify(studentRepository, never()).delete(any());
    }

    @Test
    public void givenSystemadminWithOthers_whenDeleteAccount_thenSuccessfullyDeletes() {
        student.setStudentType(StudentType.SYSTEMADMIN);
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(studentRepository.countByStudentType(StudentType.SYSTEMADMIN))
            .thenReturn(2L);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.empty());
        when(kursBeitrittsAnfrageRepository
            .findAllByStudentAndIsApprovedFalse(student))
            .thenReturn(new ArrayList<>());

        studentDashboardService.deleteAccount(STUDENT_MAIL);

        verify(studentRepository).delete(student);
    }

    @Test
    public void givenLastKursadmin_whenDeleteAccount_thenThrowsException() {
        student.setStudentType(StudentType.KURSADMIN);
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(enrollment));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> studentDashboardService.deleteAccount(STUDENT_MAIL)
        );

        assertEquals("Cannot leave course or delete account: "
            + "You are the last KURSADMIN in this course",
            exception.getMessage());
        verify(studentRepository, never()).delete(any());
    }

    @Test
    public void givenKursadminWithOthers_whenDeleteAccount_thenSuccessfullyDeletes() {
        student.setStudentType(StudentType.KURSADMIN);
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));
        when(kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedTrue(kurs))
            .thenReturn(List.of(enrollment, otherEnrollment));
        when(kursBeitrittsAnfrageRepository
            .findAllByStudentAndIsApprovedFalse(student))
            .thenReturn(new ArrayList<>());

        studentDashboardService.deleteAccount(STUDENT_MAIL);

        verify(studentRepository).delete(student);
    }

    @Test
    public void givenStudentWithoutEnrollment_whenDeleteAccount_thenSuccessfullyDeletes() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.empty());
        when(kursBeitrittsAnfrageRepository
            .findAllByStudentAndIsApprovedFalse(student))
            .thenReturn(new ArrayList<>());

        studentDashboardService.deleteAccount(STUDENT_MAIL);

        verify(studentRepository).delete(student);
    }

    @Test
    public void givenStudentWithPendingRequests_whenDeleteAccount_thenDeletesAllData() {
        KursBeitrittsAnfrage pendingRequest = new KursBeitrittsAnfrage();
        pendingRequest.setApproved(false);

        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));
        when(kursBeitrittsAnfrageRepository
            .findAllByStudentAndIsApprovedFalse(student))
            .thenReturn(List.of(pendingRequest));

        studentDashboardService.deleteAccount(STUDENT_MAIL);

        verify(kursBeitrittsAnfrageRepository).delete(enrollment);
        verify(kursBeitrittsAnfrageRepository).deleteAll(List.of(pendingRequest));
        verify(studentRepository).delete(student);
    }

    // ==================== retrieveProfile Tests ====================

    @Test
    public void givenStudentWithEnrollment_whenRetrieveProfile_thenReturnsProfileWithKurs() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));

        StudentProfileDTO profile =
            studentDashboardService.retrieveProfile(STUDENT_MAIL);

        assertNotNull(profile);
        assertEquals("Max Mustermann", profile.getName());
        assertEquals(STUDENT_MAIL, profile.getDhbwMail());
        assertEquals(StudentType.STUDENT, profile.getStudentType());
        assertEquals("TINF24B2", profile.getKursName());
    }

    @Test
    public void givenStudentWithoutEnrollment_whenRetrieveProfile_thenReturnsProfileWithNullKurs() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.empty());

        StudentProfileDTO profile =
            studentDashboardService.retrieveProfile(STUDENT_MAIL);

        assertNotNull(profile);
        assertEquals("Max Mustermann", profile.getName());
        assertEquals(STUDENT_MAIL, profile.getDhbwMail());
        assertEquals(StudentType.STUDENT, profile.getStudentType());
        assertNull(profile.getKursName());
    }

    @Test
    public void givenStudentNotFound_whenRetrieveProfile_thenThrowsException() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenThrow(new NoSuchElementException("Student not found: " + STUDENT_MAIL));

        NoSuchElementException exception = assertThrows(
            NoSuchElementException.class,
            () -> studentDashboardService.retrieveProfile(STUDENT_MAIL)
        );

        assertEquals("Student not found: " + STUDENT_MAIL, exception.getMessage());
    }

    // ==================== retrieveKursForUser Tests ====================

    @Test
    public void givenStudentWithEnrollment_whenRetrieveKursForUser_thenReturnsKursName() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));

        String kursName = studentDashboardService.retrieveKursForUser(STUDENT_MAIL);

        assertEquals("TINF24B2", kursName);
    }

    @Test
    public void givenStudentWithoutEnrollment_whenRetrieveKursForUser_thenReturnsNull() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.empty());

        String kursName = studentDashboardService.retrieveKursForUser(STUDENT_MAIL);

        assertNull(kursName);
    }

    @Test
    public void givenStudentNotFound_whenRetrieveKursForUser_thenThrowsException() {
        when(studentLookupHelper.findStudentByEmail(STUDENT_MAIL))
            .thenThrow(new NoSuchElementException("Student not found: " + STUDENT_MAIL));

        NoSuchElementException exception = assertThrows(
            NoSuchElementException.class,
            () -> studentDashboardService.retrieveKursForUser(STUDENT_MAIL)
        );

        assertEquals("Student not found: " + STUDENT_MAIL, exception.getMessage());
    }
}
