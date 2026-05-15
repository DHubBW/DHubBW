package com.se.dhubbw.kurs.application;

import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
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
public class KursMitgliederControllerIT {

    private static final String FELIX_MAIL =
        "anonym.felix.a24@student.dhbw-karlsruhe.de";

    /** Felix's student ID (KURSADMIN of TINF24B1). */
    private static final String FELIX_ID =
        "11111111-1111-1111-1111-111111111114";

    /** Anna's student ID (STUDENT member of TINF24B1). */
    private static final String ANNA_ID =
        "11111111-1111-1111-1111-111111111119";

    /** Hans's mail (second KURSADMIN of TINF24B1 for resign test). */
    private static final String HANS_MAIL =
        "resign.hans.a24@student.dhbw-karlsruhe.de";

    /** Hans's student ID (promoted to KURSADMIN at Order 24). */
    private static final String HANS_ID =
        "11111111-1111-1111-1111-111111111120";

    private static final String BASE_URL = "/kursadmin/mitglieder";

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
    @Order(1)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadmin_whenRetrieveStats_thenReturns200WithStatsDto()
            throws Exception {
        mockMvc.perform(get(BASE_URL + "/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anzahlMitglieder").isNumber())
            .andExpect(jsonPath("$.anzahlDateien").isNumber())
            .andExpect(jsonPath("$.anzahlAnfragen").isNumber());
    }

    @Test
    @Order(2)
    public void givenUnauthenticated_whenRetrieveStats_thenReturns401()
            throws Exception {
        mockMvc.perform(get(BASE_URL + "/stats"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieveStats_thenReturns403()
            throws Exception {
        mockMvc.perform(get(BASE_URL + "/stats"))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadmin_whenRetrieveMembers_thenReturns200WithArray()
            throws Exception {
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(5)
    public void givenUnauthenticated_whenRetrieveMembers_thenReturns401()
            throws Exception {
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieveMembers_thenReturns403()
            throws Exception {
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenOnlyOneKa_whenResign_thenReturns409()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/resign"))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(8)
    public void givenUnauthenticated_whenResign_thenReturns401()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/resign"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenResign_thenReturns403()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/resign"))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenAddKursadmin_thenReturns404()
            throws Exception {
        mockMvc.perform(patch(BASE_URL
            + "/add-kursadmin/"
            + "00000000-0000-0000-0000-000000000099"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    public void givenUnauthenticated_whenAddKursadmin_thenReturns401()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/add-kursadmin/" + ANNA_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenAddKursadmin_thenReturns403()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/add-kursadmin/" + ANNA_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenAnnaStudent_whenAddKursadmin_thenReturns204()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/add-kursadmin/" + ANNA_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(14)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenAlreadyKa_whenAddKursadmin_thenReturns409()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/add-kursadmin/" + ANNA_ID))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(15)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenKaMember_whenRemoveMember_thenReturns409()
            throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + ANNA_ID))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(16)
    public void givenUnauthenticated_whenRemoveKursadmin_thenReturns401()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/remove-kursadmin/" + ANNA_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(17)
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRemoveKursadmin_thenReturns403()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/remove-kursadmin/" + ANNA_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(18)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenRemoveKursadmin_thenReturns404()
            throws Exception {
        mockMvc.perform(patch(BASE_URL
            + "/remove-kursadmin/"
            + "00000000-0000-0000-0000-000000000099"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(19)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenKaMember_whenRemoveKursadmin_thenReturns200()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/remove-kursadmin/" + ANNA_ID))
            .andExpect(status().isOk());
    }

    @Test
    @Order(20)
    public void givenUnauthenticated_whenRemoveMember_thenReturns401()
            throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + ANNA_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(21)
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRemoveMember_thenReturns403()
            throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + ANNA_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(22)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenKaMember_whenRemoveFelixAsMember_thenReturns409()
            throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + FELIX_ID))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(23)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenStudentMember_whenRemoveMember_thenReturns204()
            throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + ANNA_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(24)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenHansStudent_whenAddKursadmin_thenReturns204()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/add-kursadmin/" + HANS_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(25)
    @WithMockUser(username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenRemoveMember_thenReturns404()
            throws Exception {
        mockMvc.perform(delete(BASE_URL
            + "/00000000-0000-0000-0000-000000000099"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(26)
    @WithMockUser(username = HANS_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenTwoKas_whenResign_thenReturns204()
            throws Exception {
        mockMvc.perform(patch(BASE_URL + "/resign"))
            .andExpect(status().isNoContent());
    }
}
