package com.se.dhubbw.student.domain;

import com.se.dhubbw.student.application.dto.input.LoginRequestDTO;
import com.se.dhubbw.student.application.dto.input.RegisterRequestDTO;

/**
 * Domain interface for authentication.
 * Defines operations for student registration and login.
 *
 * <p>Defines the operations required for user registration
 * and login, returning JWT tokens upon successful authentication.</p>
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface, technology-free)
 */
public interface AuthenticationService {

    /**
     * Register a new student.
     *
     * @param request the registration request containing email,
     *     name, and password
     * @return JWT token for the newly registered student
     * @throws IllegalArgumentException if the email is already registered
     */
    String registerStudent(RegisterRequestDTO request);

    /**
     * Login a student.
     *
     * @param request the login request containing email and password
     * @return JWT token for the authenticated student
     * @throws org.springframework.security.core.AuthenticationException
     *     if credentials are invalid
     */
    String login(LoginRequestDTO request);
}
