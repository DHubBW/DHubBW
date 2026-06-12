package com.se.dhubbw.student.infrastructure;

import com.se.dhubbw.shared.infrastructure.config.security.JwtService;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.application.dto.input.LoginRequestDTO;
import com.se.dhubbw.student.application.dto.input.RegisterRequestDTO;
import com.se.dhubbw.student.domain.AuthenticationService;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

/**
 * Implementation of the {@link AuthenticationService} interface.
 * Implements registration and login with password encoding, JWT
 * generation, and AuthenticationManager.
 *
 * <p>Handles password encoding, JWT token generation, and authentication
 * manager integration.</p>
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with Security integration)
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

    /**
     * Regex describing the project's password policy. Mirrors the
     * frontend's {@code PASSWORD_REGEX} so client-side and server-side
     * checks stay aligned.
     *
     * <p>Requirements: at least one lowercase letter, one uppercase
     * letter, one digit, one special character (non-letter, non-digit,
     * non-whitespace), 10..128 non-whitespace characters total.</p>
     */
    private static final String PASSWORD_REGEX_STR =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])"
            + "(?=.*[^A-Za-z0-9\\s])\\S{10,128}$";

    /**
     * Compiled form of {@link #PASSWORD_REGEX_STR}.
     */
    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile(PASSWORD_REGEX_STR);

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

        if (request.password() == null
                || !PASSWORD_PATTERN.matcher(request.password()).matches()) {
            colorLogger.colorError(
                "Registration failed: password for '{}' "
                    + "does not satisfy policy",
                request.dhbwMail());
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Passwort erfüllt die Sicherheitsanforderungen nicht");
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
