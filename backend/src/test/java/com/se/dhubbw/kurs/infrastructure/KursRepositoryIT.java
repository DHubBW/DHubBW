package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.kurs.infrastructure.db.KursRepository;
import com.se.dhubbw.shared.testing.IntegrationTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
public class KursRepositoryIT {
    private final KursRepository kursRepository;

    @Autowired
    public KursRepositoryIT(KursRepository kursRepository) {
        this.kursRepository = kursRepository;
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenKurseAreInserted() {
        // DESCRIBE & ACT
        long actual = kursRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}

