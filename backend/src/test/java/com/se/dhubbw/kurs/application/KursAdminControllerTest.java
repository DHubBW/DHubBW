package com.se.dhubbw.kurs.application;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Profile("dev")
public class KursAdminControllerTest {

    private static final String ADMIN_MAIL =
        "felix.kursadmin@dhbw-karlsruhe.de";

    private static final String LENA_ANFRAGE_ID =
        "66666666-6666-6666-6666-666666666664";

    private static final String KARL_ANFRAGE_ID =
        "66666666-6666-6666-6666-666666666665";

    private static final String MAX_ANFRAGE_ID =
        "66666666-6666-6666-6666-666666666666";

    private static final String LINUS_ANFRAGE_ID =
        "66666666-6666-6666-6666-666666666667";

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
    @WithMockUser(username = ADMIN_MAIL, roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadmin_whenRetrieve_thenReturns200WithArray()
            throws Exception {
        mockMvc.perform(get("/kursadmin/anfragen"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void givenUnauthenticated_whenRetrieve_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/kursadmin/anfragen"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieve_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/kursadmin/anfragen"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_MAIL, roles = {"KURSADMIN", "STUDENT"})
    public void givenValidRequest_whenApprove_thenReturns200()
            throws Exception {
        mockMvc.perform(patch(
            "/kursadmin/anfragen/" + LENA_ANFRAGE_ID + "/approve"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = ADMIN_MAIL, roles = {"KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenApprove_thenReturns404()
            throws Exception {
        mockMvc.perform(patch(
            "/kursadmin/anfragen/00000000-0000-0000-0000-000000000099/approve"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void givenUnauthenticated_whenApprove_thenReturns401()
            throws Exception {
        mockMvc.perform(patch(
            "/kursadmin/anfragen/" + LENA_ANFRAGE_ID + "/approve"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = ADMIN_MAIL, roles = {"KURSADMIN", "STUDENT"})
    public void givenOtherCourseRequest_whenApprove_thenReturns403()
            throws Exception {
        mockMvc.perform(patch(
            "/kursadmin/anfragen/" + MAX_ANFRAGE_ID + "/approve"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_MAIL, roles = {"KURSADMIN", "STUDENT"})
    public void givenValidRequest_whenDecline_thenReturns204()
            throws Exception {
        mockMvc.perform(delete("/kursadmin/anfragen/" + KARL_ANFRAGE_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = ADMIN_MAIL, roles = {"KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenDecline_thenReturns404()
            throws Exception {
        mockMvc.perform(delete(
            "/kursadmin/anfragen/00000000-0000-0000-0000-000000000099"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void givenUnauthenticated_whenDecline_thenReturns401()
            throws Exception {
        mockMvc.perform(delete("/kursadmin/anfragen/" + KARL_ANFRAGE_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = ADMIN_MAIL, roles = {"KURSADMIN", "STUDENT"})
    public void givenAlreadyEnrolledStudent_whenApprove_thenReturns409()
            throws Exception {
        mockMvc.perform(patch(
            "/kursadmin/anfragen/" + LINUS_ANFRAGE_ID + "/approve"))
            .andExpect(status().isConflict());
    }
}
