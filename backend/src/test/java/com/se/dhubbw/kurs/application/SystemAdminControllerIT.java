package com.se.dhubbw.kurs.application;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SystemAdminControllerIT {

    private static final String LENA_ANFRAGE_ID =
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2";

    private static final String KARL_ANFRAGE_ID =
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1";

    private static final String LINUS_ANFRAGE_ID =
        "99999999-9999-9999-9999-999999999992";

    private MockMvc mockMvc;

    private final WebApplicationContext context;

    public SystemAdminControllerIT(WebApplicationContext context) {
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
    @WithMockUser(username = "admin", roles = {"SYSTEMADMIN", "STUDENT"})
    public void givenSystemAdmin_whenRetrieveKursErstellungsanfragen_thenReturns200() throws Exception {
        mockMvc.perform(get("/systemadmin/anfragen"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "KURSADMIN")
    public void givenKursadmin_whenRetrieveKursErstellungsanfragen_thenReturns403() throws Exception {
        mockMvc.perform(get("/systemadmin/anfragen"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenRetrieveKursErstellungsanfragen_thenReturns403() throws Exception {
        mockMvc.perform(get("/systemadmin/anfragen"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticated_whenRetrieveKursErstellungsanfragen_thenReturns401() throws Exception {
        mockMvc.perform(get("/systemadmin/anfragen"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SYSTEMADMIN", "STUDENT"})
    public void givenSystemAdmin_whenApproveNonExistingRequest_thenReturns404() throws Exception {
        mockMvc.perform(patch("/systemadmin/anfragen/00000000-0000-0000-0000-000000000000/approve"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "KURSADMIN")
    public void givenKursadmin_whenApproveRequest_thenReturns403() throws Exception {
        mockMvc.perform(patch("/systemadmin/anfragen/00000000-0000-0000-0000-000000000000/approve"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenApproveRequest_thenReturns403() throws Exception {
        mockMvc.perform(patch("/systemadmin/anfragen/00000000-0000-0000-0000-000000000000/approve"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticated_whenApproveRequest_thenReturns401() throws Exception {
        mockMvc.perform(patch("/systemadmin/anfragen/00000000-0000-0000-0000-000000000000/approve"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SYSTEMADMIN", "STUDENT"})
    public void givenSystemAdmin_whenDeclineNonExistingRequest_thenReturns404() throws Exception {
        mockMvc.perform(delete("/systemadmin/anfragen/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "KURSADMIN")
    public void givenKursadmin_whenDeclineRequest_thenReturns403() throws Exception {
        mockMvc.perform(delete("/systemadmin/anfragen/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenDeclineRequest_thenReturns403() throws Exception {
        mockMvc.perform(delete("/systemadmin/anfragen/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticated_whenDeclineRequest_thenReturns401() throws Exception {
        mockMvc.perform(delete("/systemadmin/anfragen/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SYSTEMADMIN", "STUDENT"})
    public void givenValidPendingRequest_whenDeclineKursErstellungsanfrage_thenReturns204()
            throws Exception {
        mockMvc.perform(delete(
            "/systemadmin/anfragen/" + LENA_ANFRAGE_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SYSTEMADMIN", "STUDENT"})
    public void givenValidPendingRequest_whenApproveKursErstellungsanfrage_thenReturns200()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/anfragen/" + KARL_ANFRAGE_ID + "/approve"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SYSTEMADMIN", "STUDENT"})
    public void givenAlreadyEnrolledStudent_whenApproveKursErstellungsanfrage_thenReturns409()
            throws Exception {
        mockMvc.perform(patch(
            "/systemadmin/anfragen/" + LINUS_ANFRAGE_ID + "/approve"))
            .andExpect(status().isConflict());
    }
}