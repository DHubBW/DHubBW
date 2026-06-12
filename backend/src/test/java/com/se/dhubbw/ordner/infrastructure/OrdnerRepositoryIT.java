package com.se.dhubbw.ordner.infrastructure;

import com.se.dhubbw.ordner.infrastructure.db.Ordner;
import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

/**
 * Integration tests for {@link OrdnerRepository} that lock in the
 * NULL-kurs JPQL semantics: {@code findPublicRootFoldersExcludingKurs}
 * must include rows where {@code o.kurs IS NULL}, and
 * {@code findByKursAndUeberOrdnerIsNull} must not surface them via the
 * implicit-join trap.
 */
@IntegrationTest
public class OrdnerRepositoryIT {
    /**
     * UUID of the SYSTEMADMIN-created NULL-kurs platform share seeded
     * by {@code data.sql}.
     */
    private static final UUID PLATFORM_INFO_ID =
        UUID.fromString("44444444-4444-4444-4444-444444444444");

    /**
     * UUID of the TINF24B2 course used for the public-folder query.
     */
    private static final UUID TINF24B2_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222221");

    /**
     * UUID of the TINF24B1 course (used to assert that another course's
     * public root is also returned together with the NULL-kurs share).
     */
    private static final UUID TINF24B1_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final OrdnerRepository ordnerRepository;

    @Autowired
    public OrdnerRepositoryIT(OrdnerRepository ordnerRepository) {
        this.ordnerRepository = ordnerRepository;
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenOrdnerAreInserted() {
        long actual = ordnerRepository.count();
        Assertions.assertTrue(actual > 0);
    }

    /**
     * Regression: the seeded NULL-kurs public root ('Platform Info')
     * must be returned by {@code findPublicRootFoldersExcludingKurs}
     * regardless of the caller's own course UUID.
     */
    @Test
    public void
        givenNullKursPublicRoot_whenFindPublicExcludingKurs_thenIncludesIt() {
        List<Ordner> result =
            ordnerRepository.findPublicRootFoldersExcludingKurs(
                TINF24B2_ID);

        Assertions.assertTrue(
            result.stream()
                .anyMatch(o -> PLATFORM_INFO_ID.equals(o.getOrdnerID())),
            "NULL-kurs public root must be visible to TINF24B2 students");
    }

    /**
     * Regression: students of any other course also see the NULL-kurs
     * platform share. Cross-checks the LEFT JOIN does not depend on
     * the caller's own course matching anything.
     */
    @Test
    public void
        givenNullKursPublicRoot_whenQueriedFromOtherCourse_thenIncludesIt() {
        List<Ordner> result =
            ordnerRepository.findPublicRootFoldersExcludingKurs(
                TINF24B1_ID);

        Assertions.assertTrue(
            result.stream()
                .anyMatch(o -> PLATFORM_INFO_ID.equals(o.getOrdnerID())),
            "NULL-kurs public root must be visible to TINF24B1 students");
    }

    /**
     * Counter-check: the unrestricted root query
     * {@code findByUeberOrdnerIsNull} (used on the SYSTEMADMIN path)
     * also returns the NULL-kurs row.
     */
    @Test
    public void
        givenNullKursRoot_whenFindByUeberOrdnerIsNull_thenIncludesIt() {
        List<Ordner> result =
            ordnerRepository.findByUeberOrdnerIsNull();

        Assertions.assertTrue(
            result.stream()
                .anyMatch(o -> PLATFORM_INFO_ID.equals(o.getOrdnerID())),
            "SYSTEMADMIN listing must include the NULL-kurs root");
    }
}
