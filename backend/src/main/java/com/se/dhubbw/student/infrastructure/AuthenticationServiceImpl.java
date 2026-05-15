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
    public String register(final RegisterRequestDTO request) {
        if (studentRepository.findByDhbwMail(request.getDhbwMail())
            .isPresent()) {
            colorLogger.colorError(
                "Registration failed: Email '{}' already registered",
                request.getDhbwMail());
            throw new IllegalArgumentException(
                "Email already registered");
        }

        Student student = new Student();
        student.setDhbwMail(request.getDhbwMail());
        student.setName(request.getVorname() + " " + request.getNachname());
        student.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        student.setStudentType(StudentType.STUDENT);

        studentRepository.save(student);
        colorLogger.colorInfo(
            "Successfully registered new student with email: '{}'",
            request.getDhbwMail());

        UserDetails userDetails = userDetailsService
            .loadUserByUsername(request.getDhbwMail());
        return jwtService.generateToken(userDetails);
    }

    /** {@inheritDoc} */
    @Override
    public String login(final LoginRequestDTO request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getDhbwMail(),
                request.getPassword()
            )
        );

        colorLogger.colorInfo(
            "Successfully authenticated student with email: '{}'",
            request.getDhbwMail());

        UserDetails userDetails = userDetailsService
            .loadUserByUsername(request.getDhbwMail());
        return jwtService.generateToken(userDetails);
    }
}
