package com.se.dhubbw.shared.infrastructure.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for JWT token generation and validation.
 * Manages creation, signing, and verification of JSON Web Tokens for
 * authentication.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (JWT library with Spring Security
 *     integration)
 */
@Service
public class JwtService {

    /**
     * JWT secret key.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT expiration time in milliseconds.
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Extract username from JWT token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String extractUsername(final String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract a specific claim from JWT token.
     *
     * @param token          the JWT token
     * @param claimsResolver function to extract claim
     * @param <T>            the claim type
     * @return the extracted claim
     */
    public <T> T extractClaim(final String token,
                              final Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generate JWT token for user.
     *
     * @param userDetails the user details
     * @return the JWT token
     */
    public String generateToken(final UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Validate JWT token.
     *
     * @param token       the JWT token
     * @param userDetails the user details
     * @return true if valid
     */
    public boolean validateToken(final String token,
                                 final UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())
            && !isTokenExpired(token));
    }

    private String createToken(final Map<String, Object> claims,
                               final String subject) {
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }

    private Claims extractAllClaims(final String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private boolean isTokenExpired(final String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(final String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
