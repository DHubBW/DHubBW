package com.se.dhubbw.ordner.infrastructure;

import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
public class OrdnerAnlegungRepositoryIT {
    private final OrdnerAnlegungRepository ordnerAnlegungRepository;

    @Autowired
    public OrdnerAnlegungRepositoryIT(
            OrdnerAnlegungRepository ordnerAnlegungRepository) {
        this.ordnerAnlegungRepository = ordnerAnlegungRepository;
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenOrdnerAnlegungAreInserted() {
        // DESCRIBE & ACT
        long actual = ordnerAnlegungRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}

