package com.se.dhubbw.student.infrastructure.security;

import com.se.dhubbw.shared.testing.UnitTest;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    public void givenStudentUser_whenLoadByUsername_thenReturnsOnlyStudentRole() {
        when(studentRepository.findByDhbwMail("test@dhbw.de"))
            .thenReturn(Optional.of(buildStudent("test@dhbw.de", StudentType.STUDENT)));

        UserDetails result = customUserDetailsService.loadUserByUsername("test@dhbw.de");

        assertThat(result.getUsername()).isEqualTo("test@dhbw.de");
        assertThat(result.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_STUDENT");
    }

    @Test
    public void givenKursadmin_whenLoadByUsername_thenReturnsKursadminAndStudentRoles() {
        when(studentRepository.findByDhbwMail("admin@dhbw.de"))
            .thenReturn(Optional.of(buildStudent("admin@dhbw.de", StudentType.KURSADMIN)));

        UserDetails result = customUserDetailsService.loadUserByUsername("admin@dhbw.de");

        assertThat(result.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_KURSADMIN", "ROLE_STUDENT");
    }

    @Test
    public void givenSystemAdmin_whenLoadByUsername_thenReturnsAllThreeRoles() {
        when(studentRepository.findByDhbwMail("sysadmin@dhbw.de"))
            .thenReturn(Optional.of(buildStudent("sysadmin@dhbw.de", StudentType.SYSTEMADMIN)));

        UserDetails result = customUserDetailsService.loadUserByUsername("sysadmin@dhbw.de");

        assertThat(result.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_SYSTEMADMIN", "ROLE_KURSADMIN", "ROLE_STUDENT");
    }

    @Test
    public void givenUnknownUser_whenLoadByUsername_thenThrowsUsernameNotFoundException() {
        when(studentRepository.findByDhbwMail("unknown@dhbw.de"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown@dhbw.de"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found with email: unknown@dhbw.de");
    }

    private Student buildStudent(final String mail, final StudentType type) {
        Student student = new Student();
        student.setDhbwMail(mail);
        student.setPasswordHash("hash");
        student.setStudentType(type);
        return student;
    }
}
