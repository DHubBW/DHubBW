package com.se.dhubbw.ordner.infrastructure;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Profile("dev")
public class OrdnerRepositoryIT {
    private final OrdnerRepository ordnerRepository;

    @Test
    public void givenDataSQL_whenApplicationStarts_thenOrdnerAreInserted() {
        // DESCRIBE & ACT
        long actual = ordnerRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}

