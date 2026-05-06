package com.se.dhubbw.dashboard.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Profile("dev")
public class DashboardControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieveMealsForToday_thenReturns200() throws Exception {
        mockMvc.perform(get("/public/dashboard/meals/today"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void givenUnauthenticatedUser_whenRetrieveMealsForToday_thenReturns401() throws Exception {
        mockMvc.perform(get("/public/dashboard/meals/today"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SYSTEMADMIN", "STUDENT"})
    public void givenSystemAdmin_whenRetrieveLatestDateien_thenReturns200WithAllFiles() throws Exception {
        mockMvc.perform(get("/public/dashboard/dateien/latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(
        username = "linus.kronenberger@dhbw-karlsruhe.de",
        roles = "STUDENT"
    )
    public void givenApprovedStudent_whenRetrieveLatestDateien_thenReturns200WithFiles() throws Exception {
        mockMvc.perform(get("/public/dashboard/dateien/latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(
        username = "nico.anonym@dhbw-karlsruhe.de",
        roles = "STUDENT"
    )
    public void givenUnapprovedStudent_whenRetrieveLatestDateien_thenReturns403() throws Exception {
        mockMvc.perform(get("/public/dashboard/dateien/latest"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
        username = "unknown@dhbw-karlsruhe.de",
        roles = "STUDENT"
    )
    public void givenUnknownStudent_whenRetrieveLatestDateien_thenReturns403() throws Exception {
        mockMvc.perform(get("/public/dashboard/dateien/latest"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticatedUser_whenRetrieveLatestDateien_thenReturns401() throws Exception {
        mockMvc.perform(get("/public/dashboard/dateien/latest"))
            .andExpect(status().isUnauthorized());
    }
}
