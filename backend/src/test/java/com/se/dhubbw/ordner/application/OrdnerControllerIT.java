package com.se.dhubbw.ordner.application;

import com.se.dhubbw.shared.testing.IntegrationTest;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup
    .SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result
    .MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result
    .MockMvcResultMatchers.status;

/**
 * Integration tests for {@link OrdnerController}.
 *
 * <p>Tests cover GET, PUT, PATCH, and DELETE endpoints with
 * various user roles and ownership scenarios.</p>
 */
@IntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrdnerControllerIT {

    /**
     * DHBW mail of Linus (STUDENT, enrolled in TINF24B2).
     */
    private static final String LINUS_MAIL =
        "kronenberger.linus.a24@student.dhbw-karlsruhe.de";

    /**
     * DHBW mail of Nico (KURSADMIN, enrolled in TINF24B2).
     */
    private static final String NICO_MAIL =
        "anonym.nico.a24@student.dhbw-karlsruhe.de";

    /**
     * DHBW mail of Tom (SYSTEMADMIN).
     */
    private static final String TOM_MAIL =
        "anonym.tom.a24@student.dhbw-karlsruhe.de";

    /**
     * DHBW mail of Felix (KURSADMIN, enrolled in TINF24B1).
     */
    private static final String FELIX_MAIL =
        "anonym.felix.a24@student.dhbw-karlsruhe.de";

    /**
     * Root folder "Vorlesungen" in TINF24B2 (test data).
     */
    private static final String TINF24B2_ROOT_ID =
        "44444444-4444-4444-4444-444444444441";

    /**
     * Child folder "Mathematik" in TINF24B2 (test data).
     */
    private static final String TINF24B2_CHILD_ID =
        "44444444-4444-4444-4444-444444444442";

    /**
     * Root folder "Privater Ordner" in TINF24B1 (test data).
     */
    private static final String TINF24B1_ROOT_ID =
        "44444444-4444-4444-4444-444444444443";

    /**
     * UUID that does not exist in test data.
     */
    private static final String UNKNOWN_ID =
        "00000000-0000-0000-0000-000000000099";

    /** MockMvc instance used for all requests. */
    private MockMvc mockMvc;

    /**
     * UUID of the folder created in Order 30, used by the
     * delete test so it does not destroy pre-existing test data.
     */
    private static UUID createdFolderForDelete;

    /** Spring web application context. */
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Build the MockMvc instance with Spring Security before
     * each test.
     */
    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // Remove auto-generated sysadmin to ensure tests work with exact data.sql fixture
        studentRepository.findByDhbwMail("system.administrator.a24@student.dhbw-karlsruhe.de")
            .ifPresent(studentRepository::delete);
    }

    // --- GET /ordner (retrieveRootFolders) ---

    /**
     * Linus is enrolled in TINF24B2 and should receive the
     * root folders of that course.
     */
    @Test
    @Order(1)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenMember_whenGetRootFolders_thenReturns200()
            throws Exception {
        mockMvc.perform(get("/student/ordner")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    /**
     * Unauthenticated request to list root folders returns 401.
     */
    @Test
    @Order(2)
    public void givenUnauthenticated_whenGetRootFolders_thenReturns401()
            throws Exception {
        mockMvc.perform(get("/student/ordner"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Tom (SYSTEMADMIN) can retrieve all root folders across all
     * courses without a course enrollment.
     */
    @Test
    @Order(3)
    @WithMockUser(
        username = TOM_MAIL,
        roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenSysAdmin_whenGetRootFolders_thenReturns200()
            throws Exception {
        mockMvc.perform(get("/student/ordner")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // --- GET /ordner/{id}/upper ---

    /**
     * Linus can retrieve the breadcrumb for his course's child
     * folder: [root, child].
     */
    @Test
    @Order(10)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenMember_whenGetUpperFolders_thenReturnsPath()
            throws Exception {
        mockMvc.perform(
                get("/student/ordner/" + TINF24B2_CHILD_ID + "/upper")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * Linus cannot access a folder from TINF24B1 and gets 403.
     */
    @Test
    @Order(11)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenWrongCourse_whenGetUpperFolders_thenReturns403()
            throws Exception {
        mockMvc.perform(
                get("/student/ordner/" + TINF24B1_ROOT_ID + "/upper"))
            .andExpect(status().isForbidden());
    }

    /**
     * Unknown folder ID for upper path returns 404.
     */
    @Test
    @Order(12)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenUnknownId_whenGetUpperFolders_thenReturns404()
            throws Exception {
        mockMvc.perform(
                get("/student/ordner/" + UNKNOWN_ID + "/upper"))
            .andExpect(status().isNotFound());
    }

    // --- GET /ordner/{id}/children ---

    /**
     * Linus can list child folders of a root folder in his
     * course.
     */
    @Test
    @Order(20)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenMember_whenGetChildren_thenReturns200()
            throws Exception {
        mockMvc.perform(
                get("/student/ordner/" + TINF24B2_ROOT_ID + "/children")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    /**
     * Unauthenticated request to list children returns 401.
     */
    @Test
    @Order(21)
    public void givenUnauthenticated_whenGetChildren_thenReturns401()
            throws Exception {
        mockMvc.perform(
                get("/student/ordner/" + TINF24B2_ROOT_ID + "/children"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Linus (TINF24B2) cannot list children of a TINF24B1 folder.
     */
    @Test
    @Order(22)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenWrongCourse_whenGetChildren_thenReturns403()
            throws Exception {
        mockMvc.perform(
                get("/student/ordner/" + TINF24B1_ROOT_ID + "/children"))
            .andExpect(status().isForbidden());
    }

    // --- PUT /ordner (createFolder) ---

    /**
     * Linus can create a root-level folder in his course.
     * The created folder's UUID is stored for the delete test.
     */
    @Test
    @Order(30)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenMember_whenCreateRootFolder_thenReturns201()
            throws Exception {
        MvcResult result = mockMvc.perform(
                put("/student/ordner")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Neuer Ordner\","
                        + " \"isPublic\": true,"
                        + " \"farbe\": \"DEFAULT\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        String body = result.getResponse()
            .getContentAsString().replace("\"", "").trim();
        createdFolderForDelete = UUID.fromString(body);
    }

    /**
     * Linus can create a child folder under an existing folder
     * in his course.
     */
    @Test
    @Order(31)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenMember_whenCreateChildFolder_thenReturns201()
            throws Exception {
        mockMvc.perform(
                put("/student/ordner")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ueberOrdnerId\": \""
                        + TINF24B2_ROOT_ID + "\","
                        + " \"name\": \"Unterpunkt\","
                        + " \"isPublic\": true,"
                        + " \"farbe\": \"BLUE\"}"))
            .andExpect(status().isCreated());
    }

    /**
     * Unauthenticated request to create a folder returns 401.
     */
    @Test
    @Order(32)
    public void givenUnauthenticated_whenCreate_thenReturns401()
            throws Exception {
        mockMvc.perform(
                put("/student/ordner")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"X\","
                        + " \"isPublic\": true,"
                        + " \"farbe\": \"DEFAULT\"}"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Student from TINF24B2 cannot create a folder under a
     * folder that belongs to TINF24B1.
     */
    @Test
    @Order(33)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenWrongCourse_whenCreateChild_thenReturns403()
            throws Exception {
        mockMvc.perform(
                put("/student/ordner")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ueberOrdnerId\": \""
                        + TINF24B1_ROOT_ID + "\","
                        + " \"name\": \"Fremd\","
                        + " \"isPublic\": true,"
                        + " \"farbe\": \"DEFAULT\"}"))
            .andExpect(status().isForbidden());
    }

    /**
     * Tom (SYSTEMADMIN) can create a root-level folder without
     * a course enrollment.
     */
    @Test
    @Order(34)
    @WithMockUser(
        username = TOM_MAIL,
        roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenSysAdmin_whenCreateRootFolder_thenReturns201()
            throws Exception {
        mockMvc.perform(
                put("/student/ordner")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Admin Ordner\","
                        + " \"isPublic\": true,"
                        + " \"farbe\": \"DEFAULT\"}"))
            .andExpect(status().isCreated());
    }

    // --- PATCH /ordner/{id} (renameFolder) ---

    /**
     * Nico (KURSADMIN of TINF24B2) can rename a folder in his
     * course.
     */
    @Test
    @Order(40)
    @WithMockUser(
        username = NICO_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenKursAdmin_whenRename_thenReturns200()
            throws Exception {
        mockMvc.perform(
                patch("/student/ordner/" + TINF24B2_ROOT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newName\": \"Umbenannt\"}"))
            .andExpect(status().isOk());
    }

    /**
     * Unauthenticated request to rename a folder returns 401.
     */
    @Test
    @Order(41)
    public void givenUnauthenticated_whenRename_thenReturns401()
            throws Exception {
        mockMvc.perform(
                patch("/student/ordner/" + TINF24B2_ROOT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newName\": \"X\"}"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Linus (STUDENT, not creator or KURSADMIN) cannot rename
     * a folder and gets 403.
     */
    @Test
    @Order(42)
    @WithMockUser(username = LINUS_MAIL, roles = {"STUDENT"})
    public void givenNonAdminNonCreator_whenRename_thenReturns403()
            throws Exception {
        mockMvc.perform(
                patch("/student/ordner/" + TINF24B2_ROOT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newName\": \"Verboten\"}"))
            .andExpect(status().isForbidden());
    }

    /**
     * Rename with unknown folder ID returns 404.
     */
    @Test
    @Order(43)
    @WithMockUser(
        username = TOM_MAIL,
        roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenRename_thenReturns404()
            throws Exception {
        mockMvc.perform(
                patch("/student/ordner/" + UNKNOWN_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newName\": \"X\"}"))
            .andExpect(status().isNotFound());
    }

    // --- DELETE /ordner/{id} ---

    /**
     * Tom (SYSTEMADMIN) can delete a folder he did not create.
     * Uses the folder created in Order 30 to avoid destroying
     * test data that other integration tests depend on.
     */
    @Test
    @Order(50)
    @WithMockUser(
        username = TOM_MAIL,
        roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenSysAdmin_whenDelete_thenReturns204()
            throws Exception {
        mockMvc.perform(
                delete("/student/ordner/" + createdFolderForDelete))
            .andExpect(status().isNoContent());
    }

    /**
     * Unauthenticated request to delete a folder returns 401.
     */
    @Test
    @Order(51)
    public void givenUnauthenticated_whenDelete_thenReturns401()
            throws Exception {
        mockMvc.perform(
                delete("/student/ordner/" + TINF24B2_ROOT_ID))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Felix is KURSADMIN of TINF24B1 and cannot delete a folder
     * from TINF24B2.
     */
    @Test
    @Order(52)
    @WithMockUser(
        username = FELIX_MAIL,
        roles = {"KURSADMIN", "STUDENT"})
    public void givenKursAdminWrongCourse_whenDelete_thenReturns403()
            throws Exception {
        mockMvc.perform(
                delete("/student/ordner/" + TINF24B2_ROOT_ID))
            .andExpect(status().isForbidden());
    }

    /**
     * Delete with unknown folder ID returns 404.
     */
    @Test
    @Order(53)
    @WithMockUser(
        username = TOM_MAIL,
        roles = {"SYSTEMADMIN", "KURSADMIN", "STUDENT"})
    public void givenUnknownId_whenDelete_thenReturns404()
            throws Exception {
        mockMvc.perform(
                delete("/student/ordner/" + UNKNOWN_ID))
            .andExpect(status().isNotFound());
    }
}
