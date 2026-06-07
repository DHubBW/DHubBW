package com.se.dhubbw.kurs.application;

import com.se.dhubbw.kurs.application.dto.input.KursErstellungsAnfrageRequestDTO;
import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class KursControllerIT {

    private MockMvc mockMvc;

    private final WebApplicationContext context;

    private final String LINUS_MAIL = "kronenberger.linus.a24@student.dhbw-karlsruhe.de";

    private static final String PAUL_MAIL =
        "plain.paul.a24@student.dhbw-karlsruhe.de";

    private ObjectMapper objectMapper;

    public KursControllerIT(WebApplicationContext context) {
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
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieveKursNamesAndYears_thenReturns200() throws Exception {
        mockMvc.perform(get("/student/kurse"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "KURSADMIN")
    public void givenKursadmin_whenRetrieveKursNamesAndYears_thenReturns403() throws Exception {
        mockMvc.perform(get("/student/kurse"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticated_whenRetrieveKursNamesAndYears_thenReturns401() throws Exception {
        mockMvc.perform(get("/student/kurse"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenOpenJoinKursRequestWithInvalidId_thenReturns404() throws Exception {
        mockMvc.perform(post("/student/kurse/beitreten/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "KURSADMIN")
    public void givenKursadmin_whenOpenJoinKursRequest_thenReturns201Or409() throws Exception {
        mockMvc.perform(post("/student/kurse/beitreten/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void givenUnauthenticated_whenOpenJoinKursRequest_thenReturns401() throws Exception {
        mockMvc.perform(post("/student/kurse/beitreten/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(
        username = "anonym.nico.a24@student.dhbw-karlsruhe.de",
        roles = "STUDENT"
    )
    public void givenStudent_whenOpenCreateCourseRequest_thenReturns201Or409() throws Exception {
        KursErstellungsAnfrageRequestDTO request = new KursErstellungsAnfrageRequestDTO(
            "TINF99Z1",
            "2099",
            "Test notes"
        );

        mockMvc.perform(post("/student/kurse/erstellen")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser(
        username = LINUS_MAIL,
        roles = "STUDENT"
    )
    public void givenEnrolledStudent_whenOpenCreateCourseRequest_thenReturns409() throws Exception {
        KursErstellungsAnfrageRequestDTO request = new KursErstellungsAnfrageRequestDTO(
            "TINF99Y",
            "2099",
            "Should fail"
        );

        mockMvc.perform(post("/student/kurse/erstellen")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    public void givenUnauthenticated_whenOpenCreateCourseRequest_thenReturns401() throws Exception {
        KursErstellungsAnfrageRequestDTO request = new KursErstellungsAnfrageRequestDTO(
            "TINF99X",
            "2099",
            null
        );

        mockMvc.perform(post("/student/kurse/erstellen")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(
        username = PAUL_MAIL,
        roles = "STUDENT"
    )
    public void givenUnenrolledStudent_whenOpenJoinKursRequest_thenReturns201()
            throws Exception {
        mockMvc.perform(post(
            "/student/kurse/beitreten/22222222-2222-2222-2222-222222222230"))
            .andExpect(status().isCreated());
    }
}