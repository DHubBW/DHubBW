package com.se.dhubbw.kurs.infrastructure;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Profile("dev")
public class KursBeitrittsAnfrageRepositoryIT {
    private final KursBeitrittsAnfrageRepository kursBeitrittsAnfrageRepository;

    @Test
    public void givenDataSQL_whenApplicationStarts_thenKursBeitrittsAnfrageAreInserted() {
        // DESCRIBE & ACT
        long actual = kursBeitrittsAnfrageRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}

