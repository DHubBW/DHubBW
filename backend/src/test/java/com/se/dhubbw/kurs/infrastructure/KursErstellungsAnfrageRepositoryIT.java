package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrageRepository;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Profile("dev")
public class KursErstellungsAnfrageRepositoryIT {
    private final KursErstellungsAnfrageRepository kursErstellungsAnfrageRepository;

    @Test
    public void givenDataSQL_whenApplicationStarts_thenKursErstellungsAnfrageDataIsInserted() {
        // DESCRIBE & ACT
        long actual = kursErstellungsAnfrageRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}
