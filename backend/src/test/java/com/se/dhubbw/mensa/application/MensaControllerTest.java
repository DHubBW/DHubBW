package com.se.dhubbw.mensa.application;

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
public class MensaControllerTest {

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
    public void givenStudentAndExistingDate_whenRetrieveMealsForDay_thenReturns200() throws Exception {
        mockMvc.perform(get("/public/meals").param("date", "2026-05-02"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudentAndDateWithMeals_whenRetrieveMealsForDay_thenReturnsMealList() throws Exception {
        mockMvc.perform(get("/public/meals").param("date", "2026-05-02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudentAndDateWithOneMeal_whenRetrieveMealsForDay_thenReturnsSingleMeal() throws Exception {
        mockMvc.perform(get("/public/meals").param("date", "2026-05-03"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudentAndDateWithNoMeals_whenRetrieveMealsForDay_thenReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/public/meals").param("date", "2099-01-01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void givenUnauthenticatedUser_whenRetrieveMealsForDay_thenReturns401() throws Exception {
        mockMvc.perform(get("/public/meals").param("date", "2026-05-02"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudentAndMissingDate_whenRetrieveMealsForDay_thenReturns400() throws Exception {
        mockMvc.perform(get("/public/meals"))
            .andExpect(status().isBadRequest());
    }
}
