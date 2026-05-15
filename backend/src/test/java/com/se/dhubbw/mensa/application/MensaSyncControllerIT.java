package com.se.dhubbw.mensa.application;

import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class MensaSyncControllerIT {

    private MockMvc mockMvc;

    private final WebApplicationContext context;

    public MensaSyncControllerIT(WebApplicationContext context) {
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
    public void givenSystemAdmin_whenTriggerSync_thenReturns200() throws Exception {
        mockMvc.perform(post("/sysadmin/mensa/sync"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "KURSADMIN")
    public void givenKursadmin_whenTriggerSync_thenReturns403() throws Exception {
        mockMvc.perform(post("/sysadmin/mensa/sync"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void givenStudent_whenTriggerSync_thenReturns403() throws Exception {
        mockMvc.perform(post("/sysadmin/mensa/sync"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticated_whenTriggerSync_thenReturns401() throws Exception {
        mockMvc.perform(post("/sysadmin/mensa/sync"))
            .andExpect(status().isUnauthorized());
    }
}