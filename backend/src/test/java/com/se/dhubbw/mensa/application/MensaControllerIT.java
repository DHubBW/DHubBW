package com.se.dhubbw.mensa.application;

import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class MensaControllerIT {

    private MockMvc mockMvc;

    private final WebApplicationContext context;

    public MensaControllerIT(WebApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieveMealsForDay_thenReturns200() throws Exception {
        mockMvc.perform(get("/student/meals")
                .param("date", "2026-05-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "KURSADMIN")
    public void givenKursadmin_whenRetrieveMealsForDay_thenReturns403() throws Exception {
        mockMvc.perform(get("/student/meals")
                .param("date", "2026-05-15"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SYSTEMADMIN", "STUDENT"})
    public void givenSystemAdmin_whenRetrieveMealsForDay_thenReturns200() throws Exception {
        mockMvc.perform(get("/student/meals")
                .param("date", "2026-05-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void givenUnauthenticated_whenRetrieveMealsForDay_thenReturns401() throws Exception {
        mockMvc.perform(get("/student/meals")
                .param("date", "2026-05-15"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieveMealsWithoutDate_thenReturns400() throws Exception {
        mockMvc.perform(get("/student/meals"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieveCurrentWeek_thenReturns200() throws Exception {
        mockMvc.perform(get("/student/meals/current-week"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isMap());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieveNextWeek_thenReturns200() throws Exception {
        mockMvc.perform(get("/student/meals/next-week"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isMap());
    }

    @Test
    public void givenUnauthenticated_whenRetrieveCurrentWeek_thenReturns401() throws Exception {
        mockMvc.perform(get("/student/meals/current-week"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void givenUnauthenticated_whenRetrieveNextWeek_thenReturns401() throws Exception {
        mockMvc.perform(get("/student/meals/next-week"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "KURSADMIN")
    public void givenKursadmin_whenRetrieveCurrentWeek_thenReturns403() throws Exception {
        mockMvc.perform(get("/student/meals/current-week"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "KURSADMIN")
    public void givenKursadmin_whenRetrieveNextWeek_thenReturns403() throws Exception {
        mockMvc.perform(get("/student/meals/next-week"))
            .andExpect(status().isForbidden());
    }
}
