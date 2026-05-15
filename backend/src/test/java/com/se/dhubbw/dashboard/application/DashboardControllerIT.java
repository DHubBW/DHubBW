package com.se.dhubbw.dashboard.application;

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
public class DashboardControllerIT {

    private MockMvc mockMvc;

    private final WebApplicationContext context;

    public DashboardControllerIT(WebApplicationContext context) {
        this.context = context;
    }

    private final String LINUS_MAIL = "kronenberger.linus.a24@student.dhbw-karlsruhe.de";

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
        username = LINUS_MAIL,
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
        username = "",
        roles = "STUDENT"
    )
    public void givenUnapprovedStudent_whenRetrieveLatestDateien_thenReturns403() throws Exception {
        mockMvc.perform(get("/public/dashboard/dateien/latest"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
        username = "unbekannt.anonym.inf22025@student.dhbw-karlsruhe.de",
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
