package com.se.dhubbw.mensa.infrastructure;

import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
public class MensaRepositoryIT {
    private final MensaRepository mensaRepository;

    @Autowired
    public MensaRepositoryIT(MensaRepository mensaRepository) {
        this.mensaRepository = mensaRepository;
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenMealsAreInserted() {
        // DESCRIBE & ACT
        long actual = mensaRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}