package com.se.dhubbw.student.application;

import com.se.dhubbw.student.application.dto.LoginRequestDTO;
import com.se.dhubbw.student.application.dto.RegisterRequestDTO;
import com.se.dhubbw.student.domain.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    /**
     * HTTP status code for conflict (email already exists).
     */
    private static final int HTTP_CONFLICT = 409;

    /**
     * HTTP status code for unauthorized.
     */
    private static final int HTTP_UNAUTHORIZED = 401;

    /**
     * HTTP status code for bad request.
     */
    private static final int HTTP_BAD_REQUEST = 404;

    /**
     * Milliseconds per second conversion factor.
     */
    private static final int MS_PER_SECOND = 1000;

    /**
     * Authentication service.
     */
    private final AuthenticationService authenticationService;

    /**
     * Whether cookies should be secure (HTTPS only).
     */
    @Value("${jwt.cookie.secure:true}")
    private boolean cookieSecure;

    /**
     * JWT expiration time in milliseconds.
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Constructor.
     *
     * @param service the authentication service
     */
    public AuthenticationController(
        final AuthenticationService service) {
        this.authenticationService = service;
    }

    /**
     * Register a new student.
     *
     * @param request the registration request
     * @param response the HTTP response
     * @return response entity with status
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(
        @RequestBody final RegisterRequestDTO request,
        final HttpServletResponse response) {
        if (!request.getDhbwMail().matches(
            "^[a-z]+\\.[a-z]+\\.[a-z0-9]+@student\\.dhbw-karlsruhe\\.de$")) {
            return ResponseEntity.status(HTTP_BAD_REQUEST)
                .body("Invalid DHBW Mail Format");
        }
        try {
            String token = authenticationService.register(request);
            addJwtCookie(response, token);
            return ResponseEntity.ok(
                "Successfully registered student with email: '"
                    + request.getDhbwMail() + "'.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HTTP_CONFLICT)
                .body("Student with email '" + request.getDhbwMail()
                    + "' is already registered.");
        }
    }

    /**
     * Login a student.
     *
     * @param request  the login request
     * @param response the HTTP response
     * @return response entity with status
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(
        @RequestBody final LoginRequestDTO request,
        final HttpServletResponse response) {
        try {
            String token = authenticationService.login(request);
            addJwtCookie(response, token);
            return ResponseEntity.ok(
                "Successfully logged in student with email: '"
                    + request.getDhbwMail() + "'.");
        } catch (Exception e) {
            return ResponseEntity.status(HTTP_UNAUTHORIZED)
                .body("Invalid credentials for email: '"
                    + request.getDhbwMail() + "'.");
        }
    }

    private void addJwtCookie(final HttpServletResponse response,
                              final String token) {
        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(cookieSecure);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge((int) (jwtExpiration / MS_PER_SECOND));

        response.addCookie(jwtCookie);
    }

    /**
     * Test endpoint to verify JWT authentication.
     *
     * @param authentication the authentication object
     * @return user info
     */
    @GetMapping("/test")
    public ResponseEntity<String> test(final Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok("Not authenticated");
        }
        return ResponseEntity.ok(
            "Authenticated as: " + authentication.getName()
                + " with roles: " + authentication.getAuthorities()
        );
    }
}


