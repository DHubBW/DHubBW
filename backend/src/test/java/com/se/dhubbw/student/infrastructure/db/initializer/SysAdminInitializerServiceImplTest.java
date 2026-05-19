package com.se.dhubbw.student.infrastructure.db.initializer;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SysAdminInitializerServiceImpl}.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
public class SysAdminInitializerServiceImplTest {

    private static final String SYSADMIN_EMAIL = "system.administrator.a24@student.dhbw-karlsruhe.de";
    private static final String SYSADMIN_NAME = "System Administrator";
    private static final int PASSWORD_LENGTH = 20;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SysAdminInitializerServiceImpl sysAdminInitializerService;

    @Test
    public void givenNoExistingSysadmin_whenRun_thenCreatesSysadminAccount() {
        when(studentRepository.findByDhbwMail(SYSADMIN_EMAIL))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        sysAdminInitializerService.run();

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());

        Student savedStudent = studentCaptor.getValue();
        assertNotNull(savedStudent);
        assertEquals(SYSADMIN_EMAIL, savedStudent.getDhbwMail());
        assertEquals(SYSADMIN_NAME, savedStudent.getName());
        assertEquals("hashedPassword", savedStudent.getPasswordHash());
        assertEquals(StudentType.SYSTEMADMIN, savedStudent.getStudentType());
    }

    @Test
    public void givenExistingSysadmin_whenRun_thenDoesNotCreateNewAccount() {
        Student existingSysadmin = new Student();
        existingSysadmin.setDhbwMail(SYSADMIN_EMAIL);
        existingSysadmin.setName(SYSADMIN_NAME);
        existingSysadmin.setStudentType(StudentType.SYSTEMADMIN);

        when(studentRepository.findByDhbwMail(SYSADMIN_EMAIL))
                .thenReturn(Optional.of(existingSysadmin));

        sysAdminInitializerService.run();

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    public void givenPasswordGeneration_whenRun_thenGeneratesPasswordWithCorrectLength() {
        when(studentRepository.findByDhbwMail(SYSADMIN_EMAIL))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        sysAdminInitializerService.run();

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());

        String generatedPassword = passwordCaptor.getValue();
        assertNotNull(generatedPassword);
        assertEquals(PASSWORD_LENGTH, generatedPassword.length());
    }

    @Test
    public void givenPasswordGeneration_whenRun_thenPasswordContainsValidCharacters() {
        when(studentRepository.findByDhbwMail(SYSADMIN_EMAIL))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        sysAdminInitializerService.run();

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());

        String generatedPassword = passwordCaptor.getValue();
        String validChars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";

        for (char c : generatedPassword.toCharArray()) {
            assertTrue(validChars.indexOf(c) >= 0,
                    "Password contains invalid character: " + c);
        }
    }

    @Test
    public void givenMultipleRuns_whenSysadminExists_thenIdempotentBehavior() {
        Student existingSysadmin = new Student();
        existingSysadmin.setDhbwMail(SYSADMIN_EMAIL);
        existingSysadmin.setStudentType(StudentType.SYSTEMADMIN);

        when(studentRepository.findByDhbwMail(SYSADMIN_EMAIL))
                .thenReturn(Optional.of(existingSysadmin));

        // Run multiple times
        sysAdminInitializerService.run();
        sysAdminInitializerService.run();
        sysAdminInitializerService.run();

        // Should never save because sysadmin already exists
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    public void givenSysadminCreation_whenRun_thenEncodesPassword() {
        when(studentRepository.findByDhbwMail(SYSADMIN_EMAIL))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        sysAdminInitializerService.run();

        verify(passwordEncoder).encode(anyString());
    }
}
