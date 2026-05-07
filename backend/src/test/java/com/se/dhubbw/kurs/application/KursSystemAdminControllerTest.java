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
public class KursSystemAdminControllerTest {

    private static final String KARL_ANFRAGE_ID =
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1";

    private static final String LENA_ANFRAGE_ID =
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2";

    private static final String LINUS_ANFRAGE_ID =
        "99999999-9999-9999-9999-999999999992";

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
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenSystemadmin_whenRetrieve_thenReturns200WithArray()
            throws Exception {
        mockMvc.perform(get("/systemadmin/anfragen"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void givenUnauthenticated_whenRetrieve_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/systemadmin/anfragen"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieve_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/systemadmin/anfragen"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadmin_whenRetrieve_thenReturns403()
            throws Exception {
        mockMvc.perform(get("/systemadmin/anfragen"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenValidRequest_whenApprove_thenReturns200()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/anfragen/" + KARL_ANFRAGE_ID + "/approve"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenApprove_thenReturns404()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/anfragen/"
                + "00000000-0000-0000-0000-000000000099/approve"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void givenUnauthenticated_whenApprove_thenReturns401()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/anfragen/" + KARL_ANFRAGE_ID + "/approve"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenEnrolledStudent_whenApprove_thenReturns409()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/anfragen/" + LINUS_ANFRAGE_ID + "/approve"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenValidRequest_whenDecline_thenReturns204()
            throws Exception {
        mockMvc.perform(delete(
            "/systemadmin/anfragen/" + LENA_ANFRAGE_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenDecline_thenReturns404()
            throws Exception {
        mockMvc.perform(delete(
            "/systemadmin/anfragen/"
                + "00000000-0000-0000-0000-000000000099"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void givenUnauthenticated_whenDecline_thenReturns401()
            throws Exception {
        mockMvc.perform(delete(
            "/systemadmin/anfragen/" + LENA_ANFRAGE_ID))
            .andExpect(status().isUnauthorized());
    }
}
