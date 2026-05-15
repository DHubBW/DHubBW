package com.se.dhubbw.kurs.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import com.se.dhubbw.shared.testing.IntegrationTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SystemAdminVerwaltungControllerIT {

    private static final String TINF26C1_ID =
        "22222222-2222-2222-2222-222222222230";

    private static final String TOM_STUDENT_ID =
        "11111111-1111-1111-1111-111111111113";

    private static final String EVA_STUDENT_ID =
        "11111111-1111-1111-1111-111111111121";

    private static final String PAUL_STUDENT_ID =
        "11111111-1111-1111-1111-111111111122";

    private static final String UNKNOWN_ID =
        "00000000-0000-0000-0000-000000000099";

    private static final String TINF24B2_ID =
        "22222222-2222-2222-2222-222222222221";

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

    // --- GET /systemadmin/stats ---

    @Test
    @Order(1)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenSystemadmin_whenRetrieveStats_thenReturns200()
            throws Exception {
        mockMvc.perform(get("/systemadmin/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anzahlKurse").isNumber())
            .andExpect(jsonPath("$.anzahlNutzer").isNumber())
            .andExpect(jsonPath("$.anzahlDateien").isNumber())
            .andExpect(jsonPath("$.megabyteBelegt").isNumber())
            .andExpect(jsonPath("$.megabyteVerfuegbar").isNumber())
            .andExpect(
                jsonPath("$.anzahlKurserstellungsanfragen")
                    .isNumber());
    }

    @Test
    @Order(2)
    public void givenUnauthenticated_whenRetrieveStats_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/systemadmin/stats"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieveStats_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/systemadmin/stats"))
            .andExpect(status().isForbidden());
    }

    // --- GET /systemadmin/kurse ---

    @Test
    @Order(4)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenSystemadmin_whenRetrieveKurse_thenReturns200()
            throws Exception {
        mockMvc.perform(get("/systemadmin/kurse"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(5)
    public void givenUnauthenticated_whenRetrieveKurse_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/systemadmin/kurse"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @WithMockUser(roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadmin_whenRetrieveKurse_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/systemadmin/kurse"))
            .andExpect(status().isForbidden());
    }

    // --- DELETE /systemadmin/kurse/{kursId} ---

    @Test
    @Order(7)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenUnknownKursId_whenDeleteKurs_thenReturns404()
            throws Exception {
        mockMvc.perform(delete("/systemadmin/kurse/" + UNKNOWN_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    public void givenUnauthenticated_whenDeleteKurs_thenReturns401()
            throws Exception {
        mockMvc.perform(delete("/systemadmin/kurse/" + TINF26C1_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @WithMockUser(roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadmin_whenDeleteKurs_thenReturns403()
            throws Exception {
        mockMvc.perform(delete("/systemadmin/kurse/" + TINF26C1_ID))
            .andExpect(status().isForbidden());
    }

    // --- GET /systemadmin/kurse/{kursId}/mitglieder ---

    @Test
    @Order(10)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenValidKurs_whenRetrieveMembers_thenReturns200()
            throws Exception {
        mockMvc.perform(get(
            "/systemadmin/kurse/" + TINF24B2_ID + "/mitglieder"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(11)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenUnknownKursId_whenRetrieveMembers_thenReturns404()
            throws Exception {
        mockMvc.perform(get(
            "/systemadmin/kurse/" + UNKNOWN_ID + "/mitglieder"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(12)
    public void givenUnauthenticated_whenRetrieveMembers_thenReturns401()
            throws Exception {
        mockMvc.perform(get(
            "/systemadmin/kurse/" + TINF24B2_ID + "/mitglieder"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    @WithMockUser(roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadmin_whenRetrieveMembers_thenReturns403()
            throws Exception {
        mockMvc.perform(get(
            "/systemadmin/kurse/" + TINF24B2_ID + "/mitglieder"))
            .andExpect(status().isForbidden());
    }

    // --- GET /systemadmin/studenten ---

    @Test
    @Order(14)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenSystemadmin_whenRetrieveStudenten_thenReturns200()
            throws Exception {
        mockMvc.perform(get("/systemadmin/studenten"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(15)
    public void givenUnauthenticated_whenRetrieveStudenten_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/systemadmin/studenten"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(16)
    @WithMockUser(roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadmin_whenRetrieveStudenten_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/systemadmin/studenten"))
            .andExpect(status().isForbidden());
    }

    // --- PATCH /systemadmin/studenten/{studentId}/add-sysadmin ---

    @Test
    @Order(17)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenUnknownStudentId_whenAddSysAdmin_thenReturns404()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + UNKNOWN_ID
                + "/add-sysadmin"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(18)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenAlreadySysAdmin_whenAddSysAdmin_thenReturns409()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + TOM_STUDENT_ID
                + "/add-sysadmin"))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(19)
    public void givenUnauthenticated_whenAddSysAdmin_thenReturns401()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + PAUL_STUDENT_ID
                + "/add-sysadmin"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(20)
    @WithMockUser(roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadmin_whenAddSysAdmin_thenReturns403()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + PAUL_STUDENT_ID
                + "/add-sysadmin"))
            .andExpect(status().isForbidden());
    }

    // --- PATCH /systemadmin/studenten/{studentId}/remove-sysadmin ---

    @Test
    @Order(21)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenUnknownStudentId_whenRemoveSysAdmin_thenReturns404()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + UNKNOWN_ID
                + "/remove-sysadmin"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(22)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenNotSysAdmin_whenRemoveSysAdmin_thenReturns400()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + PAUL_STUDENT_ID
                + "/remove-sysadmin"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Order(23)
    public void givenUnauthenticated_whenRemoveSysAdmin_thenReturns401()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + EVA_STUDENT_ID
                + "/remove-sysadmin"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(24)
    @WithMockUser(roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadmin_whenRemoveSysAdmin_thenReturns403()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + EVA_STUDENT_ID
                + "/remove-sysadmin"))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(25)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenTwoSysAdmins_whenRemoveSysAdmin_thenReturns200()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + EVA_STUDENT_ID
                + "/remove-sysadmin"))
            .andExpect(status().isOk());
    }

    @Test
    @Order(26)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenOneSysAdmin_whenRemoveSysAdmin_thenReturns409()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + TOM_STUDENT_ID
                + "/remove-sysadmin"))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(27)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenStudent_whenAddSysAdmin_thenReturns200()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/studenten/" + PAUL_STUDENT_ID
                + "/add-sysadmin"))
            .andExpect(status().isOk());
    }

    @Test
    @Order(28)
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenValidKurs_whenDeleteKurs_thenReturns204()
            throws Exception {
        mockMvc.perform(delete("/systemadmin/kurse/" + TINF26C1_ID))
            .andExpect(status().isNoContent());
    }
}
