package com.se.dhubbw.shared.infrastructure.config.security;

import com.se.dhubbw.shared.testing.UnitTest;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    public void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
            "ThisIsASecretKeyForTestingPurposesMinimumSizeIs256BitsLong!");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);
    }

    @Test
    public void givenValidUser_whenGenerateToken_thenExtractUsernameSucceeds() {
        UserDetails userDetails = new User("test@dhbw.de", "password",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token)).isEqualTo("test@dhbw.de");
    }

    @Test
    public void givenValidToken_whenValidate_thenReturnsTrue() {
        UserDetails userDetails = new User("test@dhbw.de", "password",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.validateToken(token, userDetails)).isTrue();
    }

    @Test
    public void givenTokenForDifferentUser_whenValidate_thenReturnsFalse() {
        UserDetails user1 = new User("user1@dhbw.de", "password",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        UserDetails user2 = new User("user2@dhbw.de", "password",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        String token = jwtService.generateToken(user1);

        assertThat(jwtService.validateToken(token, user2)).isFalse();
    }

    @Test
    public void givenExpiredToken_whenValidate_thenThrowsExpiredJwtException() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        UserDetails userDetails = new User("test@dhbw.de", "password",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        String expiredToken = jwtService.generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.validateToken(expiredToken, userDetails))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void givenUserWithMultipleRoles_whenGenerateToken_thenRolesIncluded() {
        UserDetails userDetails = new User("admin@dhbw.de", "password",
            List.of(
                new SimpleGrantedAuthority("ROLE_SYSTEMADMIN"),
                new SimpleGrantedAuthority("ROLE_KURSADMIN"),
                new SimpleGrantedAuthority("ROLE_STUDENT")
            ));

        String token = jwtService.generateToken(userDetails);
        List<String> roles = jwtService.extractClaim(token,
            claims -> (List<String>) claims.get("roles"));

        assertThat(roles).containsExactlyInAnyOrder(
            "ROLE_SYSTEMADMIN", "ROLE_KURSADMIN", "ROLE_STUDENT");
    }
}
