package com.se.dhubbw.student.application;

import com.se.dhubbw.shared.testing.IntegrationTest;
import com.se.dhubbw.student.application.dto.input.LoginRequestDTO;
import com.se.dhubbw.student.application.dto.input.RegisterRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class AuthenticationControllerIT {

    private MockMvc mockMvc;

    private final WebApplicationContext context;

    private ObjectMapper objectMapper;

    private final String LINUS_MAIL = "kronenberger.linus.a24@student.dhbw-karlsruhe.de";

    public AuthenticationControllerIT(WebApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void givenValidRegistration_whenRegister_thenReturns200AndSetsCookie() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
            "nachname.vorname.a24@student.dhbw-karlsruhe.de",
            "Test Student",
            "SecurePassword123!"
        );

        mockMvc.perform(post("/public/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("jwt"))
            .andExpect(cookie().httpOnly("jwt", true))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Successfully registered student with email")));
    }

    @Test
    public void givenMailWithoutdhbwKarlsruhe_whenRegister_thenReturns422() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
            "nachname.vorname.a24@wrong.de",
            "Linus Kronenberger",
            "password"
        );

        mockMvc.perform(post("/public/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is(422));
    }

    @Test
    public void givenExistingEmail_whenRegister_thenReturns409() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
            "kronenberger.linus.a24@student.dhbw-karlsruhe.de",
            "Duplicate Kronenberger",
            "password"
        );

        mockMvc.perform(post("/public/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "is already registered")));
    }

    @Test
    public void givenInvalidCredentials_whenLogin_thenReturns401() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO(
            "kronenberger.linus.a24@student.dhbw-karlsruhe.de",
            "wrongpassword"
        );

        mockMvc.perform(post("/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Invalid credentials")));
    }

    @Test
    public void givenNonExistentUser_whenLogin_thenReturns401() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO(
            "nonexistent.linus.a24@student.dhbw-karlsruhe.de",
            "password"
        );

        mockMvc.perform(post("/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Invalid credentials")));
    }

    @Test
    @WithMockUser(
        username = LINUS_MAIL,
        roles = "STUDENT"
    )
    public void givenAuthenticatedUser_whenTest_thenReturns200WithUserInfo() throws Exception {
        mockMvc.perform(get("/public/auth/test"))
            .andExpect(status().isOk());
    }

    @Test
    public void givenUnauthenticatedUser_whenTest_thenReturns401() throws Exception {
        mockMvc.perform(get("/public/auth/test"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void givenInvalidNameFormat_whenRegister_thenReturns422()
            throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
            "nachname.vorname.a24@student.dhbw-karlsruhe.de",
            "linus kronenberger",
            "password"
        );

        mockMvc.perform(post("/public/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is(422));
    }

    @Test
    public void givenValidCredentials_whenLogin_thenReturns200AndSetsCookie()
            throws Exception {
        String loginMail = "test.login.a24@student.dhbw-karlsruhe.de";
        String loginPassword = "LoginTestPassword123!";

        mockMvc.perform(post("/public/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequestDTO(
                    loginMail, "Linus Kronenberger", loginPassword))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequestDTO(loginMail, loginPassword))))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("jwt"))
            .andExpect(cookie().httpOnly("jwt", true))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Successfully logged in student with email")));
    }

    @Test
    public void givenValidLogin_thenReturnsTokenInBodyAndSetsCookie()
            throws Exception {
        String loginMail = "token.login.a24@student.dhbw-karlsruhe.de";
        String loginPassword = "TokenBodyTest123!";

        mockMvc.perform(post("/public/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequestDTO(
                    loginMail, "Token Test", loginPassword))))
            .andExpect(status().isOk());

        long beforeLogin = System.currentTimeMillis();
        mockMvc.perform(post("/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequestDTO(loginMail, loginPassword))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.message",
                containsString("Successfully logged in")))
            .andExpect(jsonPath("$.expiresAt", greaterThan(beforeLogin)))
            .andExpect(cookie().exists("jwt"))
            .andExpect(cookie().httpOnly("jwt", true));
    }

    @Test
    public void givenValidRegister_thenReturnsTokenInBodyAndSetsCookie()
            throws Exception {
        String mail =
            "token.register.a24@student.dhbw-karlsruhe.de";
        long beforeRegister = System.currentTimeMillis();

        mockMvc.perform(post("/public/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequestDTO(
                    mail, "Token Register", "RegisterTest123!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.message",
                containsString("Successfully registered")))
            .andExpect(jsonPath("$.expiresAt", greaterThan(beforeRegister)))
            .andExpect(cookie().exists("jwt"))
            .andExpect(cookie().httpOnly("jwt", true));
    }

    @Test
    public void givenValidLogin_thenSetsSameSiteLaxAttribute()
            throws Exception {
        String loginMail = "samesite.test.a24@student.dhbw-karlsruhe.de";
        String loginPassword = "SameSiteTest123!";

        mockMvc.perform(post("/public/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequestDTO(
                    loginMail, "Samesite Tester", loginPassword))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequestDTO(loginMail, loginPassword))))
            .andExpect(status().isOk())
            .andExpect(header().string("Set-Cookie",
                containsString("SameSite=Lax")));
    }
}