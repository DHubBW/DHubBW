package com.se.dhubbw.datei.application;

import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.setup
    .SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result
    .MockMvcResultMatchers.status;

@IntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DateiControllerIT {

    private static final String FELIX_MAIL =
        "anonym.felix.a24@student.dhbw-karlsruhe.de";

    private static final String NICO_MAIL =
        "anonym.nico.a24@student.dhbw-karlsruhe.de";

    private static final String TOM_MAIL =
        "anonym.tom.a24@student.dhbw-karlsruhe.de";

    private static final String LINUS_MAIL =
        "kronenberger.linus.a24@student.dhbw-karlsruhe.de";

    /** Folder belonging to TINF24B1, Felix is KURSADMIN. */
    private static final String TINF24B1_ORDNER_ID =
        "44444444-4444-4444-4444-444444444443";

    /** Folder belonging to TINF24B2 (Mathematik), Linus is member. */
    private static final String TINF24B2_ORDNER_ID =
        "44444444-4444-4444-4444-444444444442";

    /** File uploaded by Nico in TINF24B2 folder. */
    private static final String DATEI_1_ID =
        "55555555-5555-5555-5555-555555555551";

    /** File uploaded by Linus in TINF24B1 folder. */
    private static final String DATEI_2_ID =
        "55555555-5555-5555-5555-555555555552";

    private static final String UNKNOWN_ID =
        "00000000-0000-0000-0000-000000000099";

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

    // --- Upload tests ---

    @Test
    @Order(1)
    @WithMockUser(
            username = FELIX_MAIL,
            roles = {"KURSADMIN", "STUDENT"})
    public void givenFelixKursadmin_whenUpload_thenReturns201()
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf",
            "application/octet-stream",
            "test content".getBytes());

        mockMvc.perform(
            multipart(HttpMethod.PUT, "/student/dateien/upload")
                .file(file)
                .param("ueberordnerId", TINF24B1_ORDNER_ID)
                .param("name", "test.pdf"))
            .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    public void givenUnauthenticated_whenUpload_thenReturns401()
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf",
            "application/octet-stream",
            "test content".getBytes());

        mockMvc.perform(
            multipart(HttpMethod.PUT, "/student/dateien/upload")
                .file(file)
                .param("ueberordnerId", TINF24B1_ORDNER_ID)
                .param("name", "test.pdf"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenNonMember_whenUpload_thenReturns403()
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf",
            "application/octet-stream",
            "test content".getBytes());

        mockMvc.perform(
            multipart(HttpMethod.PUT, "/student/dateien/upload")
                .file(file)
                .param("ueberordnerId", TINF24B1_ORDNER_ID)
                .param("name", "test.pdf"))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @WithMockUser(
            username = FELIX_MAIL,
            roles = {"KURSADMIN", "STUDENT"})
    public void givenUnknownFolder_whenUpload_thenReturns404()
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf",
            "application/octet-stream",
            "test content".getBytes());

        mockMvc.perform(
            multipart(HttpMethod.PUT, "/student/dateien/upload")
                .file(file)
                .param("ueberordnerId", UNKNOWN_ID)
                .param("name", "test.pdf"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @WithMockUser(
            username = NICO_MAIL,
            roles = {"KURSADMIN", "STUDENT"})
    public void givenKursadminWrongCourse_whenUpload_thenReturns403()
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf",
            "application/octet-stream",
            "test content".getBytes());

        mockMvc.perform(
            multipart(HttpMethod.PUT, "/student/dateien/upload")
                .file(file)
                .param("ueberordnerId", TINF24B1_ORDNER_ID)
                .param("name", "test.pdf"))
            .andExpect(status().isForbidden());
    }

    // --- Rename tests ---

    @Test
    @Order(10)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenMember_whenRename_thenReturns200()
            throws Exception {
        mockMvc.perform(
            patch("/student/dateien/" + DATEI_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newName\": \"umbenennt.pdf\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @Order(11)
    public void givenUnauthenticated_whenRename_thenReturns401()
            throws Exception {
        mockMvc.perform(
            patch("/student/dateien/" + DATEI_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newName\": \"new.pdf\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @WithMockUser(
            username = FELIX_MAIL,
            roles = {"KURSADMIN", "STUDENT"})
    public void givenNonMember_whenRename_thenReturns403()
            throws Exception {
        mockMvc.perform(
            patch("/student/dateien/" + DATEI_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newName\": \"new.pdf\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @WithMockUser(
            username = NICO_MAIL,
            roles = {"KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenRename_thenReturns404()
            throws Exception {
        mockMvc.perform(
            patch("/student/dateien/" + UNKNOWN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newName\": \"new.pdf\"}"))
            .andExpect(status().isNotFound());
    }

    // --- Delete tests ---

    @Test
    @Order(20)
    @WithMockUser(
            username = FELIX_MAIL,
            roles = {"KURSADMIN", "STUDENT"})
    public void givenMember_whenDelete_thenReturns204()
            throws Exception {
        mockMvc.perform(delete("/student/dateien/" + DATEI_2_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(21)
    public void givenUnauthenticated_whenDelete_thenReturns401()
            throws Exception {
        mockMvc.perform(delete("/student/dateien/" + DATEI_1_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(22)
    @WithMockUser(
            username = FELIX_MAIL,
            roles = {"KURSADMIN", "STUDENT"})
    public void givenNonMember_whenDelete_thenReturns403()
            throws Exception {
        mockMvc.perform(delete("/student/dateien/" + DATEI_1_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(23)
    @WithMockUser(
            username = TOM_MAIL,
            roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenDelete_thenReturns404()
            throws Exception {
        mockMvc.perform(delete("/student/dateien/" + UNKNOWN_ID))
            .andExpect(status().isNotFound());
    }

    // --- RetrieveDateienForOrdner tests ---

    @Test
    @Order(30)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenMember_whenGetDateienForOrdner_thenReturns200()
            throws Exception {
        mockMvc.perform(
            get("/student/dateien/ordner/" + TINF24B2_ORDNER_ID))
            .andExpect(status().isOk());
    }

    @Test
    @Order(31)
    public void givenNoAuth_whenGetDateienForOrdner_thenReturns401()
            throws Exception {
        mockMvc.perform(
            get("/student/dateien/ordner/" + TINF24B2_ORDNER_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(32)
    @WithMockUser(
            username = FELIX_MAIL,
            roles = {"KURSADMIN", "STUDENT"})
    public void givenNonMember_whenGetDateienForOrdner_thenReturns403()
            throws Exception {
        mockMvc.perform(
            get("/student/dateien/ordner/" + TINF24B2_ORDNER_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(33)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenUnknownId_whenGetDateienForOrdner_thenReturns404()
            throws Exception {
        mockMvc.perform(
            get("/student/dateien/ordner/" + UNKNOWN_ID))
            .andExpect(status().isNotFound());
    }

    // --- RetrieveAllFiles tests ---

    @Test
    @Order(40)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenMember_whenGetAllFiles_thenReturns200()
            throws Exception {
        mockMvc.perform(get("/student/dateien"))
            .andExpect(status().isOk());
    }

    @Test
    @Order(41)
    public void givenNoAuth_whenGetAllFiles_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/student/dateien"))
            .andExpect(status().isUnauthorized());
    }

    // --- DownloadDatei tests ---

    @Test
    @Order(50)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenMember_whenDownloadDatei_thenReturns200()
            throws Exception {
        mockMvc.perform(
            get("/student/dateien/" + DATEI_1_ID + "/download"))
            .andExpect(status().isOk());
    }

    @Test
    @Order(51)
    public void givenNoAuth_whenDownloadDatei_thenReturns401()
            throws Exception {
        mockMvc.perform(
            get("/student/dateien/" + DATEI_1_ID + "/download"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(52)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenUnknownId_whenDownloadDatei_thenReturns404()
            throws Exception {
        mockMvc.perform(
            get("/student/dateien/" + UNKNOWN_ID + "/download"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(53)
    @WithMockUser(
            username = FELIX_MAIL,
            roles = {"KURSADMIN", "STUDENT"})
    public void givenNonMember_whenDownloadDatei_thenReturns403()
            throws Exception {
        mockMvc.perform(
            get("/student/dateien/" + DATEI_1_ID + "/download"))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(54)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenTwoDownloads_whenList_thenCounterIncrementedByTwo()
            throws Exception {
        int before = readDownloadAnzahl();

        mockMvc.perform(
            get("/student/dateien/" + DATEI_1_ID + "/download"))
            .andExpect(status().isOk());
        mockMvc.perform(
            get("/student/dateien/" + DATEI_1_ID + "/download"))
            .andExpect(status().isOk());

        int after = readDownloadAnzahl();
        org.junit.jupiter.api.Assertions.assertEquals(
            before + 2, after);
    }

    private int readDownloadAnzahl() throws Exception {
        String body = mockMvc.perform(
            get("/student/dateien/ordner/" + TINF24B2_ORDNER_ID))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        com.fasterxml.jackson.databind.JsonNode array =
            new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body);
        for (com.fasterxml.jackson.databind.JsonNode node : array) {
            if (DATEI_1_ID.equals(
                    node.get("dateiID").asText())) {
                return node.get("downloadAnzahl").asInt();
            }
        }
        throw new IllegalStateException(
            "DATEI_1_ID not found in folder listing");
    }
}
