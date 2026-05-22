package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.shared.infrastructure.helper.StudentLookupHelper;
import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.infrastructure.db.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class KursStudentHelperTest {

    @Mock
    private StudentLookupHelper studentLookupHelper;

    @Mock
    private KursBeitrittsAnfrageRepository kursBeitrittsAnfrageRepository;

    @InjectMocks
    private KursStudentHelper kursStudentHelper;

    private Student student;
    private Kurs kurs;

    @BeforeEach
    public void setUp() {
        student = new Student();
        student.setStudentID(UUID.randomUUID());
        student.setDhbwMail("linus@dhbw.de");

        kurs = new Kurs();
        kurs.setKursID(UUID.randomUUID());
        kurs.setKursName("TINF24B2");
    }

    @Test
    public void givenKnownEmail_whenFindByEmail_thenDelegatesToHelper() {
        when(studentLookupHelper.findStudentByEmail("linus@dhbw.de"))
            .thenReturn(student);

        Student result =
            kursStudentHelper.findStudentByEmail("linus@dhbw.de");

        assertEquals(student, result);
    }

    @Test
    public void givenUnknownEmail_whenFindByEmail_thenThrows() {
        when(studentLookupHelper.findStudentByEmail("unknown@dhbw.de"))
            .thenThrow(new NoSuchElementException("Student not found"));

        assertThrows(NoSuchElementException.class,
            () -> kursStudentHelper.findStudentByEmail("unknown@dhbw.de"));
    }

    @Test
    public void givenKnownId_whenFindById_thenDelegatesToHelper() {
        UUID id = student.getStudentID();
        when(studentLookupHelper.findStudentById(id)).thenReturn(student);

        Student result = kursStudentHelper.findStudentById(id);

        assertEquals(student, result);
    }

    @Test
    public void givenUnknownId_whenFindById_thenThrows() {
        UUID unknownId = UUID.randomUUID();
        when(studentLookupHelper.findStudentById(unknownId))
            .thenThrow(new NoSuchElementException("Student not found"));

        assertThrows(NoSuchElementException.class,
            () -> kursStudentHelper.findStudentById(unknownId));
    }

    @Test
    public void givenEnrolledStudent_whenFindKursForStudent_thenReturnsKurs() {
        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setStudent(student);
        enrollment.setKurs(kurs);

        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));

        Kurs result = kursStudentHelper.findKursForStudent(student);

        assertEquals(kurs, result);
    }

    @Test
    public void givenNoEnrollment_whenFindKursForStudent_thenThrows() {
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> kursStudentHelper.findKursForStudent(student));
    }

    @Test
    public void givenAdminEmail_whenFindKursForAdminByEmail_thenReturnsKurs() {
        KursBeitrittsAnfrage enrollment = new KursBeitrittsAnfrage();
        enrollment.setStudent(student);
        enrollment.setKurs(kurs);

        when(studentLookupHelper.findStudentByEmail("linus@dhbw.de"))
            .thenReturn(student);
        when(kursBeitrittsAnfrageRepository
            .findByStudentAndIsApprovedTrue(student))
            .thenReturn(Optional.of(enrollment));

        Kurs result =
            kursStudentHelper.findKursForAdminByEmail("linus@dhbw.de");

        assertEquals(kurs, result);
    }

    @Test
    public void givenUnknownEmail_whenFindKursForAdminByEmail_thenThrows() {
        when(studentLookupHelper.findStudentByEmail("unknown@dhbw.de"))
            .thenThrow(new NoSuchElementException("Student not found"));

        assertThrows(NoSuchElementException.class,
            () -> kursStudentHelper
                .findKursForAdminByEmail("unknown@dhbw.de"));
    }
}
