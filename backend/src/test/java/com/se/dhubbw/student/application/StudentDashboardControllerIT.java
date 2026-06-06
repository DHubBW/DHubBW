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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
        mockMvc.perform(get("/student/dashboard/leave-kurs"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Successfully left course")));
    }

    @Test
    @WithMockUser(username = "nonexistent@student.dhbw-karlsruhe.de",
        roles = {"STUDENT"})
    public void givenNonExistentStudent_whenLeaveKurs_thenReturns404()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/leave-kurs"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "has no course enrollment")));
    }

    @Test
    @WithMockUser(username = KURSADMIN_MAIL,
        roles = {"STUDENT", "KURSADMIN"})
    public void givenLastKursadmin_whenLeaveKurs_thenReturns400()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/leave-kurs"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "last KURSADMIN")));
    }

    @Test
    public void givenUnauthenticated_whenLeaveKurs_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/leave-kurs"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== deleteAccount Tests ====================

    @Test
    @WithMockUser(username = "anonym.max.a24@student.dhbw-karlsruhe.de",
        roles = {"STUDENT"})
    public void givenStudentWithoutEnrollment_whenDeleteAccount_thenReturns200()
            throws Exception {
        mockMvc.perform(delete("/student/dashboard/delete-account"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Successfully deleted account")));
    }

    @Test
    @WithMockUser(username = KURSADMIN_MAIL,
        roles = {"STUDENT", "KURSADMIN"})
    public void givenLastKursadmin_whenDeleteAccount_thenReturns400()
            throws Exception {
        mockMvc.perform(delete("/student/dashboard/delete-account"))
            .andExpect(status().is4xxClientError())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "last KURSADMIN")));
    }

    @Test
    @WithMockUser(username = SYSTEMADMIN_MAIL,
        roles = {"STUDENT", "KURSADMIN", "SYSTEMADMIN"})
    public void givenSystemadminWithOthers_whenDeleteAccount_thenReturns200()
            throws Exception {
        // Tom is not the last systemadmin (Eva exists)
        mockMvc.perform(delete("/student/dashboard/delete-account"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Successfully deleted account")));
    }

    @Test
    public void givenUnauthenticated_whenDeleteAccount_thenReturns401()
            throws Exception {
        mockMvc.perform(delete("/student/dashboard/delete-account"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {})
    public void givenNoRole_whenLeaveKurs_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/leave-kurs"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {})
    public void givenNoRole_whenDeleteAccount_thenReturns403()
            throws Exception {
        mockMvc.perform(delete("/student/dashboard/delete-account"))
            .andExpect(status().isForbidden());
    }

    // ==================== retrieveProfile Tests ====================

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {"STUDENT"})
    public void givenAuthenticatedStudent_whenRetrieveProfile_thenReturns200WithProfile()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Linus Kronenberger"))
            .andExpect(jsonPath("$.dhbwMail").value(STUDENT_MAIL))
            .andExpect(jsonPath("$.studentType").value("STUDENT"))
            .andExpect(jsonPath("$.kursName").value("TINF24B2"));
    }

    @Test
    @WithMockUser(username = "anonym.max.a24@student.dhbw-karlsruhe.de",
        roles = {"STUDENT"})
    public void givenStudentWithoutEnrollment_whenRetrieveProfile_thenReturnsProfileWithNullKurs()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Max"))
            .andExpect(jsonPath("$.kursName").isEmpty());
    }

    // ==================== retrieveProfile pendingKursRequest Tests ====================

    @Test
    @WithMockUser(username = "plain.paul.a24@student.dhbw-karlsruhe.de",
        roles = {"STUDENT"})
    public void givenStudentWithNoPendingRequest_whenRetrieveProfile_thenPendingIsNull()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingKursRequest").doesNotExist());
    }

    @Test
    @WithMockUser(username = "anonym.max.a24@student.dhbw-karlsruhe.de",
        roles = {"STUDENT"})
    public void givenStudentWithPendingJoinRequest_whenRetrieveProfile_thenReturnsBeitritt()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingKursRequest.type").value("BEITRITT"))
            .andExpect(jsonPath("$.pendingKursRequest.kursName")
                .value("TINF24B2"));
    }

    @Test
    @WithMockUser(username = "pending.peter.a24@student.dhbw-karlsruhe.de",
        roles = {"STUDENT"})
    public void givenStudentWithPendingCreationRequest_whenRetrieveProfile_thenReturnsErstellung()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingKursRequest.type")
                .value("ERSTELLUNG"))
            .andExpect(jsonPath("$.pendingKursRequest.kursName")
                .value("TINF26D1"));
    }

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {"STUDENT"})
    public void givenStudentWithBothRequestTypes_whenRetrieveProfile_thenReturnsBeitritt()
            throws Exception {
        // Linus has both a pending join request and a pending creation
        // request — join request takes precedence
        mockMvc.perform(get("/student/dashboard/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingKursRequest.type").value("BEITRITT"));
    }

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {})
    public void givenNoRole_whenRetrieveProfile_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/profile"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticated_whenRetrieveProfile_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/profile"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== logout Tests ====================

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {"STUDENT"})
    public void givenAuthenticatedStudent_whenLogout_thenReturns200AndInvalidatesCookie()
            throws Exception {
        mockMvc.perform(post("/student/dashboard/logout"))
            .andExpect(status().isOk())
            .andExpect(cookie().maxAge("jwt", 0));
    }

    @Test
    public void givenUnauthenticated_whenLogout_thenReturns401()
            throws Exception {
        mockMvc.perform(post("/student/dashboard/logout"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {})
    public void givenNoRole_whenLogout_thenReturns403()
            throws Exception {
        mockMvc.perform(post("/student/dashboard/logout"))
            .andExpect(status().isForbidden());
    }

    // ==================== retrieveKursForUser Tests ====================

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {"STUDENT"})
    public void givenStudentWithEnrollment_whenRetrieveKursForUser_thenReturnsKursName()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/kurs"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "anonym.max.a24@student.dhbw-karlsruhe.de",
        roles = {"STUDENT"})
    public void givenStudentWithoutEnrollment_whenRetrieveKursForUser_thenReturnsKeinKurs()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/kurs"))
            .andExpect(status().isOk());
    }

    @Test
    public void givenUnauthenticated_whenRetrieveKursForUser_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/kurs"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = STUDENT_MAIL, roles = {})
    public void givenNoRole_whenRetrieveKursForUser_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/student/dashboard/kurs"))
            .andExpect(status().isForbidden());
    }
}
