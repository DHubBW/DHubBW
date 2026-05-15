package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrageRepository;
import com.se.dhubbw.shared.testing.IntegrationTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
public class KursErstellungsAnfrageRepositoryIT {
    private final KursErstellungsAnfrageRepository
            kursErstellungsAnfrageRepository;

    @Autowired
    public KursErstellungsAnfrageRepositoryIT(
            KursErstellungsAnfrageRepository kursErstellungsAnfrageRepository) {
        this.kursErstellungsAnfrageRepository = kursErstellungsAnfrageRepository;
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenKursErstellungsAnfrageDataIsInserted() {
        // DESCRIBE & ACT
        long actual = kursErstellungsAnfrageRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}
