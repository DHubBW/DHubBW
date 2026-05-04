package com.se.dhubbw.mensa.infrastructure;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Profile("dev")
public class MensaRepositoryIT {
    private final MensaRepository mensaRepository;

    @Test
    public void givenDataSQL_whenApplicationStarts_thenMealsAreInserted() {
        // DESCRIBE & ACT
        long actual = mensaRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}