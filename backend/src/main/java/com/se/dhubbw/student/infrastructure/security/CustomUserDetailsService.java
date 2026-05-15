package com.se.dhubbw.student.infrastructure.security;

import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import lombok.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom UserDetailsService implementation for student authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Color logger instance.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(CustomUserDetailsService.class);

    /**
     * Student repository.
     */
    private final StudentRepository studentRepository;

    /**
     * Constructor.
     *
     * @param repository the student repository
     */
    public CustomUserDetailsService(final StudentRepository repository) {
        this.studentRepository = repository;
    }

    /**
     * Load user by DHBW mail address.
     *
     * @param username the DHBW mail (username)
     * @return UserDetails with roles
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull final String username)
        throws UsernameNotFoundException {
        Student student = studentRepository.findByDhbwMail(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found with email: " + username));

        colorLogger.colorInfo("Loading user: {} with StudentType: {}",
            username, student.getStudentType());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(
            "ROLE_" + student.getStudentType().name()));

        // Add hierarchical roles
        switch (student.getStudentType()) {
            case SYSTEMADMIN -> authorities.addAll(List.of(
                new SimpleGrantedAuthority("ROLE_KURSADMIN"),
                new SimpleGrantedAuthority("ROLE_STUDENT")
            ));
            case KURSADMIN -> authorities.add(
                new SimpleGrantedAuthority("ROLE_STUDENT")
            );
            default -> { }
        }

        colorLogger.colorInfo("Loaded authorities: {}", authorities);

        return new User(
            student.getDhbwMail(),
            student.getPasswordHash(),
            authorities
        );
    }
}
