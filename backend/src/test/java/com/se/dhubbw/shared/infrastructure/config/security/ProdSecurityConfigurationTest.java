package com.se.dhubbw.shared.infrastructure.config.security;

import com.se.dhubbw.shared.testing.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration
    .AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProdSecurityConfiguration}.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
public class ProdSecurityConfigurationTest {

    /**
     * Mocked JWT authentication filter.
     */
    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Mocked user details service.
     */
    @Mock
    private UserDetailsService userDetailsService;

    /**
     * Instance under test.
     */
    private ProdSecurityConfiguration prodConfig;

    /**
     * Creates a fresh ProdSecurityConfiguration before each test.
     */
    @BeforeEach
    public void setUp() {
        prodConfig = new ProdSecurityConfiguration(
            jwtAuthenticationFilter, userDetailsService);
    }

    /**
     * Password encoder should be a BCryptPasswordEncoder instance.
     */
    @Test
    public void givenConfig_whenGetPasswordEncoder_thenReturnsBCryptEncoder() {
        PasswordEncoder encoder = prodConfig.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    /**
     * Authentication provider should be a DaoAuthenticationProvider.
     */
    @Test
    public void givenConfig_whenGetAuthenticationProvider_thenReturnsDaoProvider() {
        AuthenticationProvider provider = prodConfig.authenticationProvider();

        assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
    }

    /**
     * Authentication manager should delegate to AuthenticationConfiguration.
     */
    @Test
    public void givenAuthConfig_whenGetAuthenticationManager_thenDelegatesToConfig()
            throws Exception {
        AuthenticationConfiguration authConfig =
            mock(AuthenticationConfiguration.class);
        AuthenticationManager mockManager =
            mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(mockManager);

        AuthenticationManager result =
            prodConfig.authenticationManager(authConfig);

        assertThat(result).isSameAs(mockManager);
    }
}
