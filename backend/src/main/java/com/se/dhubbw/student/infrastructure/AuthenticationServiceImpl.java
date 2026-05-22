package com.se.dhubbw.student.infrastructure;

import com.se.dhubbw.shared.infrastructure.config.security.JwtService;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.application.dto.LoginRequestDTO;
import com.se.dhubbw.student.application.dto.RegisterRequestDTO;
import com.se.dhubbw.student.domain.AuthenticationService;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of authentication service for student registration and login.
 *
 * <p>Handles password encoding, JWT token generation, and authentication
 * manager integration.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    /**
     * Color logger instance.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(AuthenticationServiceImpl.class);

    /**
     * Student repository.
     */
    private final StudentRepository studentRepository;

    /**
     * Password encoder.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * JWT service.
     */
    private final JwtService jwtService;

    /**
     * Authentication manager.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * User details service.
     */
    private final UserDetailsService userDetailsService;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public String registerStudent(final RegisterRequestDTO request) {
        if (studentRepository.findByDhbwMail(request.dhbwMail())
            .isPresent()) {
            colorLogger.colorError(
                "Registration failed: Email '{}' already registered",
                request.dhbwMail());
            throw new IllegalArgumentException(
                "Email already registered");
        }

        // generate the new registered student
        Student registeredStudent = new Student();
        registeredStudent.setDhbwMail(request.dhbwMail());
        registeredStudent.setName(request.name());
        registeredStudent.setPasswordHash(
            passwordEncoder.encode(request.password()));
        registeredStudent.setStudentType(StudentType.STUDENT);

        // save the new student
        studentRepository.save(registeredStudent);

        colorLogger.colorInfo(
            "Successfully registered new student with email: '{}'",
            request.dhbwMail());

        UserDetails userDetails = userDetailsService
            .loadUserByUsername(request.dhbwMail());
        return jwtService.generateToken(userDetails);
    }

    /** {@inheritDoc} */
    @Override
    public String login(final LoginRequestDTO request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.dhbwMail(),
                request.password()
            )
        );

        colorLogger.colorInfo(
            "Successfully authenticated student with email: '{}'",
            request.dhbwMail());

        UserDetails userDetails = userDetailsService
            .loadUserByUsername(request.dhbwMail());
        return jwtService.generateToken(userDetails);
    }
}
