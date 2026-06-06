package com.se.dhubbw.shared.infrastructure.config.security;

import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter for incoming HTTP requests.
 * Extracts and validates JWT tokens from cookies or Authorization header.
 *
 * <p>Reads the JWT from a cookie named {@code jwt} first; if no
 * cookie is present, falls back to the {@code Authorization: Bearer}
 * header. This allows browser-based flows (cookie) and API clients
 * such as Swagger UI (header) to authenticate transparently.</p>
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Security Filter)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Length of the {@code "Bearer "} prefix in the Authorization
     * header value.
     */
    private static final int BEARER_PREFIX_LENGTH = 7;

    /**
     * JWT service.
     */
    private final JwtService jwtService;

    /**
     * User details service.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Color logger instance.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(this.getClass());

    /**
     * Constructor.
     *
     * @param jwt         the JWT service
     * @param userDetails the user details service
     */
    public JwtAuthenticationFilter(final JwtService jwt,
                                   final UserDetailsService userDetails) {
        this.jwtService = jwt;
        this.userDetailsService = userDetails;
    }

    /**
     * Filter incoming requests and validate JWT.
     *
     * <p>The JWT is resolved from the {@code jwt} cookie first.
     * If no cookie is present, the {@code Authorization: Bearer}
     * header is tried as a fallback so that API clients such as
     * Swagger UI can authenticate without relying on cookies.</p>
     *
     * <p>Skips authentication for register and login endpoints.</p>
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if servlet error occurs
     * @throws IOException      if I/O error occurs
     */
    @Override
    protected void doFilterInternal(
        final HttpServletRequest request,
        final @NonNull HttpServletResponse response,
        final @NonNull FilterChain filterChain)
        throws ServletException, IOException {

        // Skip JWT validation for auth endpoints
        String path = request.getRequestURI();
        if (path.equals("/public/auth/register")
            || path.equals("/public/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = null;
        String username = null;

        // Extract JWT from cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    colorLogger.colorDebug(
                        "JWT cookie found for request: {}",
                        path);
                    break;
                }
            }
        }

        // Fallback: read JWT from Authorization: Bearer header
        if (jwt == null) {
            String authHeader =
                request.getHeader("Authorization");
            if (authHeader != null
                    && authHeader.startsWith("Bearer ")) {
                jwt = authHeader
                    .substring(BEARER_PREFIX_LENGTH);
                colorLogger.colorDebug(
                    "JWT Bearer header found for request: {}",
                    path);
            }
        }

        // Extract username from token
        if (jwt != null) {
            try {
                username = jwtService.extractUsername(jwt);
                colorLogger.colorDebug("Username extracted from JWT: {}",
                    username);
            } catch (Exception e) {
                colorLogger.colorDebug("JWT validation failed: {}",
                    e.getMessage());
            }
        }

        // Authenticate user if token is valid
        if (username != null
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService
                .loadUserByUsername(username);

            if (jwtService.validateToken(jwt, userDetails)) {
                colorLogger.colorInfo(
                    "JWT token validated successfully for user: {}",
                    username);
                colorLogger.colorDebug("User authorities: {}",
                    userDetails.getAuthorities());
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                authToken.setDetails(
                    new WebAuthenticationDetailsSource()
                        .buildDetails(request)
                );
                SecurityContextHolder.getContext()
                    .setAuthentication(authToken);
                colorLogger.colorDebug(
                    "Authentication set in SecurityContext for user: {}",
                    username);
            } else {
                colorLogger.colorError(
                    "JWT token validation failed for user: {}",
                    username);
            }
        }

        filterChain.doFilter(request, response);
    }
}
