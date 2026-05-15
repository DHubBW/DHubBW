package com.se.dhubbw.student.application;

import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link StudentDashboardController}.
 */
@IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StudentDashboardControllerIT {

    private MockMvc mockMvc;

    private final WebApplicationContext context;

    private static final String STUDENT_MAIL =
        "kronenberger.linus.a24@student.dhbw-karlsruhe.de";
    private static final String KURSADMIN_MAIL =
        "anonym.felix.a24@student.dhbw-karlsruhe.de";
    private static final String SYSTEMADMIN_MAIL =
        "anonym.tom.a24@student.dhbw-karlsruhe.de";

    public StudentDashboardControllerIT(final WebApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    // ==================== leaveKurs Tests ====================

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {"STUDENT"})
    public void givenAuthenticatedStudent_whenLeaveKurs_thenReturns200()
            throws Exception {
        mockMvc.perform(get("/dashboard/leave-kurs"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Successfully left course")));
    }

    @Test
    @WithMockUser(username = "nonexistent@student.dhbw-karlsruhe.de",
        roles = {"STUDENT"})
    public void givenNonExistentStudent_whenLeaveKurs_thenReturns404()
            throws Exception {
        mockMvc.perform(get("/dashboard/leave-kurs"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "has no course enrollment")));
    }

    @Test
    @WithMockUser(username = KURSADMIN_MAIL,
        roles = {"STUDENT", "KURSADMIN"})
    public void givenLastKursadmin_whenLeaveKurs_thenReturns400()
            throws Exception {
        mockMvc.perform(get("/dashboard/leave-kurs"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "last KURSADMIN")));
    }

    @Test
    public void givenUnauthenticated_whenLeaveKurs_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/dashboard/leave-kurs"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== deleteAccount Tests ====================

    @Test
    @WithMockUser(username = "anonym.max.a24@student.dhbw-karlsruhe.de",
        roles = {"STUDENT"})
    public void givenStudentWithoutEnrollment_whenDeleteAccount_thenReturns200()
            throws Exception {
        mockMvc.perform(delete("/dashboard/delete-account"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Successfully deleted account")));
    }

    @Test
    @WithMockUser(username = KURSADMIN_MAIL,
        roles = {"STUDENT", "KURSADMIN"})
    public void givenLastKursadmin_whenDeleteAccount_thenReturns400()
            throws Exception {
        mockMvc.perform(delete("/dashboard/delete-account"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "last KURSADMIN")));
    }

    @Test
    @WithMockUser(username = SYSTEMADMIN_MAIL,
        roles = {"STUDENT", "KURSADMIN", "SYSTEMADMIN"})
    public void givenSystemadminWithOthers_whenDeleteAccount_thenReturns200()
            throws Exception {
        // Tom is not the last systemadmin (Eva exists)
        mockMvc.perform(delete("/dashboard/delete-account"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Successfully deleted account")));
    }

    @Test
    public void givenUnauthenticated_whenDeleteAccount_thenReturns401()
            throws Exception {
        mockMvc.perform(delete("/dashboard/delete-account"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {})
    public void givenNoRole_whenLeaveKurs_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/dashboard/leave-kurs"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {})
    public void givenNoRole_whenDeleteAccount_thenReturns403()
            throws Exception {
        mockMvc.perform(delete("/dashboard/delete-account"))
            .andExpect(status().isForbidden());
    }
}
