package com.se.dhubbw.student.application;

import com.se.dhubbw.shared.application.dto.SmallResponseDTO;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import com.se.dhubbw.student.application.dto.LoginRequestDTO;
import com.se.dhubbw.student.application.dto.RegisterRequestDTO;
import com.se.dhubbw.student.domain.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
     * ColorLogger for errors / log messages.
     */
    private final ColorLogger colorLogger = new ColorLogger(this.getClass());

    /**
     * HTTP status code for conflict (email already exists).
     */
    private static final int HTTP_CONFLICT = 409;

    /**
     * HTTP status code for unauthorized.
     */
    private static final int HTTP_UNAUTHORIZED = 401;

    /**
     * HTTP status code for wrong format of mail / name.
     */
    private static final int HTTP_UNPROCESSABLE_ENTITY = 422;

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
    public ResponseEntity<SmallResponseDTO> register(
        @RequestBody final RegisterRequestDTO request,
        final HttpServletResponse response) {
        if (!request.dhbwMail().matches(
            "^[a-z]+\\.[a-z]+\\.[a-z0-9]+@student\\.dhbw-karlsruhe\\.de$")) {
            colorLogger.colorError("The dhbw mail format "
                + "of the student doesn't match the required format.");
            return ResponseEntity.status(HTTP_UNPROCESSABLE_ENTITY)
                .body(new SmallResponseDTO("Invalid DHBW Mail Format"));
        } else if (!request.name()
            .matches("^[A-ZÄÖÜ][a-zäöüß]+(?:[- ][A-ZÄÖÜ]"
                + "[a-zäöüß]+)*\\s+[A-ZÄÖÜ][a-zäöüß]"
                + "+(?:[- ][A-ZÄÖÜ][a-zäöüß]+)*$")) {
            colorLogger.colorError("The name of the student "
                + "doesn't match the required format 'FirstName LastName'.");
            return ResponseEntity.status(HTTP_UNPROCESSABLE_ENTITY)
                .body(new SmallResponseDTO("Invalid Name format. "
                    + "The name has to be in the following format: "
                    + "'FirstName LastName'"));
        }
        try {
            String token = authenticationService.registerStudent(request);
            addJwtCookie(response, token);
            return ResponseEntity.status(HttpStatus.OK).body(
                new SmallResponseDTO("Successfully registered "
                    + "student with email: '"
                    + request.dhbwMail() + "'."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HTTP_CONFLICT)
                .body(new SmallResponseDTO("Student with email '"
                    + request.dhbwMail()
                    + "' is already registered."));
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
    public ResponseEntity<SmallResponseDTO> login(
        @RequestBody final LoginRequestDTO request,
        final HttpServletResponse response) {
        try {
            String token = authenticationService.login(request);
            addJwtCookie(response, token);
            return ResponseEntity.status(HttpStatus.OK)
                .body(new SmallResponseDTO(
                "Successfully logged in student with email: '"
                + request.dhbwMail() + "'."));
        } catch (Exception e) {
            return ResponseEntity
                .status(HTTP_UNAUTHORIZED)
                .body(new SmallResponseDTO(
                    "Invalid credentials for email: '\"\n"
                    + "                    + request.dhbwMail() + \"'.\""));
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
    public ResponseEntity<SmallResponseDTO> test(
        final Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.OK)
                .body(new SmallResponseDTO("Not authenticated"));
        }
        return ResponseEntity.status(HttpStatus.OK)
            .body(new SmallResponseDTO("Authenticated as: "
                + authentication.getName()
            + " with roles: " + authentication.getAuthorities()));
    }
}


