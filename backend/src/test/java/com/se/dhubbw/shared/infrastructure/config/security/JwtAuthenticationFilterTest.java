package com.se.dhubbw.shared.infrastructure.config.security;

import com.se.dhubbw.shared.testing.UnitTest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void givenRegisterEndpoint_whenDoFilter_thenSkipsJwtValidation() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    public void givenLoginEndpoint_whenDoFilter_thenSkipsJwtValidation() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    public void givenNoCookies_whenDoFilter_thenProceedsWithoutAuthentication() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    public void givenCookiesWithoutJwt_whenDoFilter_thenProceedsWithoutAuthentication()
        throws Exception {
        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies())
            .thenReturn(new Cookie[]{new Cookie("other", "value")});

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    public void givenValidJwtCookie_whenDoFilter_thenSetsAuthentication() throws Exception {
        String jwtToken = "valid.jwt.token";
        String username = "user@dhbw.de";
        UserDetails userDetails = new User(username, "password",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("jwt", jwtToken)});
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(false)).thenReturn(null);
        when(jwtService.extractUsername(jwtToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.validateToken(jwtToken, userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(username);
    }

    @Test
    public void givenInvalidJwtToken_whenDoFilter_thenDoesNotSetAuthentication() throws Exception {
        String jwtToken = "invalid.jwt.token";
        String username = "user@dhbw.de";
        UserDetails userDetails = new User(username, "password",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("jwt", jwtToken)});
        when(jwtService.extractUsername(jwtToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.validateToken(jwtToken, userDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    public void givenMalformedJwtToken_whenDoFilter_thenProceedsWithoutAuthentication()
        throws Exception {
        String jwtToken = "malformed.jwt.token";

        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("jwt", jwtToken)});
        when(jwtService.extractUsername(jwtToken))
            .thenThrow(new RuntimeException("Invalid token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    public void givenAlreadyAuthenticated_whenDoFilter_thenDoesNotOverwriteAuthentication()
        throws Exception {
        Authentication existingAuth = new UsernamePasswordAuthenticationToken(
            "existing@dhbw.de", null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        String jwtToken = "some.jwt.token";
        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("jwt", jwtToken)});
        when(jwtService.extractUsername(jwtToken)).thenReturn("user@dhbw.de");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication())
            .isEqualTo(existingAuth);
    }

    @Test
    public void givenNoCookieButValidBearerHeader_whenDoFilter_thenSetsAuthentication()
        throws Exception {
        String jwtToken = "valid.jwt.token";
        String username = "user@dhbw.de";
        UserDetails userDetails = new User(username, "password",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization"))
            .thenReturn("Bearer " + jwtToken);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(false)).thenReturn(null);
        when(jwtService.extractUsername(jwtToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username))
            .thenReturn(userDetails);
        when(jwtService.validateToken(jwtToken, userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(username);
    }

    @Test
    public void givenNonBearerAuthorizationHeader_whenDoFilter_thenNoAuthentication()
        throws Exception {
        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization"))
            .thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(
            SecurityContextHolder.getContext().getAuthentication()
        ).isNull();
    }

    @Test
    public void givenBearerHeaderWithInvalidJwt_whenDoFilter_thenNoAuthentication()
        throws Exception {
        String jwtToken = "invalid.jwt.token";
        String username = "user@dhbw.de";
        UserDetails userDetails = new User(username, "password",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization"))
            .thenReturn("Bearer " + jwtToken);
        when(jwtService.extractUsername(jwtToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username))
            .thenReturn(userDetails);
        when(jwtService.validateToken(jwtToken, userDetails))
            .thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(
            SecurityContextHolder.getContext().getAuthentication()
        ).isNull();
    }

    @Test
    public void givenBearerHeaderWithMalformedJwt_whenDoFilter_thenNoAuthentication()
        throws Exception {
        String jwtToken = "malformed.jwt.token";

        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization"))
            .thenReturn("Bearer " + jwtToken);
        when(jwtService.extractUsername(jwtToken))
            .thenThrow(new RuntimeException("Invalid token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(
            SecurityContextHolder.getContext().getAuthentication()
        ).isNull();
    }

    @Test
    public void givenBothCookieAndBearerHeader_whenDoFilter_thenCookieTakesPrecedence()
        throws Exception {
        String cookieToken = "cookie.jwt.token";
        String username = "user@dhbw.de";
        UserDetails userDetails = new User(username, "password",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getCookies())
            .thenReturn(new Cookie[]{new Cookie("jwt", cookieToken)});
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(false)).thenReturn(null);
        when(jwtService.extractUsername(cookieToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username))
            .thenReturn(userDetails);
        when(jwtService.validateToken(cookieToken, userDetails))
            .thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(username);
    }
}
