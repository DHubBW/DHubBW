package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.kurs.application.dto.KursYearDTO;
import com.se.dhubbw.kurs.domain.KursService;
import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Integration tests for {@link KursServiceImpl}.
 *
 * <p>Runs against the H2 in-memory database seeded by
 * {@code data.sql}. Every test method rolls back automatically
 * due to {@link Transactional}.</p>
 *
 * <p>Fixture state from data.sql:</p>
 * <ul>
 *   <li>Linus – has an approved beitrittsanfrage (enrolled)</li>
 *   <li>Nico – has a pending (not approved) beitrittsanfrage</li>
 *   <li>Tom – has no requests at all</li>
 * </ul>
 */
@IntegrationTest
@Transactional
public class KursServiceIT {

    /** DHBW e-mail of a student who is already enrolled. */
    private static final String LINUS_MAIL =
        "kronenberger.linus.a24@student.dhbw-karlsruhe.de";

    /** DHBW e-mail of a student with a pending join request. */
    private static final String NICO_MAIL =
        "anonym.nico.a24@student.dhbw-karlsruhe.de";

    /** DHBW e-mail of a student with no requests at all. */
    private static final String TOM_MAIL =
        "anonym.tom.a24@student.dhbw-karlsruhe.de";

    /** UUID of TINF24B2 (see data.sql). */
    private static final UUID KURS_TINF24B2_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222221");

    private final KursService kursService;

    @Autowired
    public KursServiceIT(KursService kursService) {
        this.kursService = kursService;
    }

    // ---- retrieveKursNamesAndYears ----

    /**
     * Verifies that the data.sql seed contains at least two courses.
     */
    @Test
    public void givenDataSQL_whenRetrieveKursNamesAndYears_thenAtLeastTwo() {
        // ACT
        List<KursYearDTO> result = kursService.retrieveKursNamesAndYears();
        // ASSERT
        Assertions.assertTrue(result.size() >= 2);
    }

    /**
     * Verifies that every returned DTO has a non-null ID, name, and year.
     */
    @Test
    public void givenDataSQL_whenRetrieveKursNamesAndYears_thenDtosValid() {
        // ACT
        List<KursYearDTO> result = kursService.retrieveKursNamesAndYears();
        // ASSERT
        for (KursYearDTO dto : result) {
            Assertions.assertNotNull(dto.getKursID());
            Assertions.assertNotNull(dto.getKursName());
            Assertions.assertNotNull(dto.getJahrgang());
        }
    }

    // ---- openJoinKursRequest ----

    /**
     * A student with no prior requests may open a join request.
     */
    @Test
    public void givenStudentNoEnrollment_whenOpenJoinKursRequest_thenOk() {
        // Tom has no beitrittsanfrage in data.sql
        // ACT + ASSERT (no exception expected)
        Assertions.assertDoesNotThrow(() ->
            kursService.openJoinKursRequest(
                KURS_TINF24B2_ID, TOM_MAIL));
    }

    /**
     * A student who is already enrolled receives 409 on join request.
     */
    @Test
    public void givenAlreadyApproved_whenOpenJoinKursRequest_thenConflict() {
        // Linus already has an approved beitrittsanfrage in data.sql
        // ACT + ASSERT
        ResponseStatusException ex = Assertions.assertThrows(
            ResponseStatusException.class,
            () -> kursService.openJoinKursRequest(
                KURS_TINF24B2_ID, LINUS_MAIL));
        Assertions.assertEquals(409, ex.getStatusCode().value());
    }

    /**
     * A student with a pending join request receives 409 on new request.
     */
    @Test
    public void givenOpenRequest_whenOpenJoinKursRequest_thenConflict() {
        // Nico has a pending (not approved) beitrittsanfrage in data.sql
        // ACT + ASSERT
        ResponseStatusException ex = Assertions.assertThrows(
            ResponseStatusException.class,
            () -> kursService.openJoinKursRequest(
                KURS_TINF24B2_ID, NICO_MAIL));
        Assertions.assertEquals(409, ex.getStatusCode().value());
    }

    /**
     * An unknown course UUID results in a 404 response.
     */
    @Test
    public void givenUnknownKursId_whenOpenJoinKursRequest_thenNotFound() {
        // ACT + ASSERT
        ResponseStatusException ex = Assertions.assertThrows(
            ResponseStatusException.class,
            () -> kursService.openJoinKursRequest(
                UUID.randomUUID(), TOM_MAIL));
        Assertions.assertEquals(404, ex.getStatusCode().value());
    }

    /**
     * An unknown student e-mail results in a 404 response.
     */
    @Test
    public void givenUnknownStudent_whenOpenJoinKursRequest_thenNotFound() {
        // ACT + ASSERT
        ResponseStatusException ex = Assertions.assertThrows(
            ResponseStatusException.class,
            () -> kursService.openJoinKursRequest(
                KURS_TINF24B2_ID, "nobody.user.test123@student.dhbw-karlsruhe.de"));
        Assertions.assertEquals(404, ex.getStatusCode().value());
    }

    // ---- openCreateCourseRequest ----

    /**
     * A student with no requests may open a course creation request.
     */
    @Test
    public void givenNewCourse_whenOpenCreateCourseRequest_thenOk() {
        // "WINF26A1" + "2026" does not exist in data.sql, Tom is free
        // ACT + ASSERT
        Assertions.assertDoesNotThrow(() ->
            kursService.openCreateCourseRequest(
                "TINF26A1", "2026", "Bitte erstellen!", TOM_MAIL));
    }

    /**
     * A second creation request while one is open results in 409.
     */
    @Test
    public void givenOpenCreation_whenOpenCreateCourseRequest_thenConflict() {
        // Create a first creation request for Tom (no existing requests)
        kursService.openCreateCourseRequest(
            "TINF25A1", "2099", null, TOM_MAIL);
        // Second request must be rejected (409)
        ResponseStatusException ex = Assertions.assertThrows(
            ResponseStatusException.class,
            () -> kursService.openCreateCourseRequest(
                "WINF99A2", "2099", null, TOM_MAIL));
        Assertions.assertEquals(409, ex.getStatusCode().value());
    }

    /**
     * A student who is already enrolled receives 409 on creation request.
     */
    @Test
    public void givenEnrolled_whenOpenCreateCourseRequest_thenConflict() {
        // Linus has an approved beitritt in data.sql
        // ACT + ASSERT
        ResponseStatusException ex = Assertions.assertThrows(
            ResponseStatusException.class,
            () -> kursService.openCreateCourseRequest(
                "WINF26A1", "2026", null, LINUS_MAIL));
        Assertions.assertEquals(409, ex.getStatusCode().value());
    }

    /**
     * Requesting creation of an already-existing course returns 409.
     */
    @Test
    public void givenExistingCourse_whenOpenCreateCourseRequest_thenConflict() {
        // TINF24B2 + 2024 already exists in data.sql
        // ACT + ASSERT
        ResponseStatusException ex = Assertions.assertThrows(
            ResponseStatusException.class,
            () -> kursService.openCreateCourseRequest(
                "TINF24B2", "2024", null, TOM_MAIL));
        Assertions.assertEquals(409, ex.getStatusCode().value());
    }

    /**
     * Null notes are replaced with the default message; no exception thrown.
     */
    @Test
    public void givenNullNotes_whenOpenCreateCourseRequest_thenOk() {
        // ACT + ASSERT
        Assertions.assertDoesNotThrow(() ->
            kursService.openCreateCourseRequest(
                "TINF26A1", "2026", null, TOM_MAIL));
    }

    /**
     * An unknown student e-mail results in a 404 response.
     */
    @Test
    public void givenUnknownStudent_whenOpenCreateCourseRequest_thenNotFound() {
        // ACT + ASSERT
        ResponseStatusException ex = Assertions.assertThrows(
            ResponseStatusException.class,
            () -> kursService.openCreateCourseRequest(
                "TINF26A1", "2026", null, "nobody.user.test123@student.dhbw-karlsruhe.de"));
        Assertions.assertEquals(404, ex.getStatusCode().value());
    }
}
