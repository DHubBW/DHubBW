package com.se.dhubbw.shared.infrastructure.helper;

import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
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
public class StudentLookupHelperTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentLookupHelper studentLookupHelper;

    private Student student;

    @BeforeEach
    public void setUp() {
        student = new Student();
        student.setStudentID(UUID.randomUUID());
        student.setName("Linus");
        student.setDhbwMail("linus@dhbw.de");
    }

    @Test
    public void givenKnownEmail_whenFindByEmail_thenReturnsStudent() {
        when(studentRepository.findByDhbwMail("linus@dhbw.de"))
            .thenReturn(Optional.of(student));

        Student result =
            studentLookupHelper.findStudentByEmail("linus@dhbw.de");

        assertEquals(student, result);
    }

    @Test
    public void givenUnknownEmail_whenFindByEmail_thenThrows() {
        when(studentRepository.findByDhbwMail("unknown@dhbw.de"))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> studentLookupHelper
                .findStudentByEmail("unknown@dhbw.de"));
    }

    @Test
    public void givenKnownId_whenFindById_thenReturnsStudent() {
        UUID id = student.getStudentID();
        when(studentRepository.findById(id))
            .thenReturn(Optional.of(student));

        Student result = studentLookupHelper.findStudentById(id);

        assertEquals(student, result);
    }

    @Test
    public void givenUnknownId_whenFindById_thenThrows() {
        UUID unknownId = UUID.randomUUID();
        when(studentRepository.findById(unknownId))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> studentLookupHelper.findStudentById(unknownId));
    }
}
