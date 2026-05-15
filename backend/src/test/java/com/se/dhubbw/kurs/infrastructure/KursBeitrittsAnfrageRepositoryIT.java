package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.shared.testing.IntegrationTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
public class KursBeitrittsAnfrageRepositoryIT {
    private final KursBeitrittsAnfrageRepository
            kursBeitrittsAnfrageRepository;

    @Autowired
    public KursBeitrittsAnfrageRepositoryIT(
            KursBeitrittsAnfrageRepository kursBeitrittsAnfrageRepository) {
        this.kursBeitrittsAnfrageRepository = kursBeitrittsAnfrageRepository;
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenKursBeitrittsAnfrageAreInserted() {
        // DESCRIBE & ACT
        long actual = kursBeitrittsAnfrageRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}

