package com.se.dhubbw.ordner.infrastructure;

import com.se.dhubbw.ordner.infrastructure.db.OrdnerRepository;
import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
public class OrdnerRepositoryIT {
    private final OrdnerRepository ordnerRepository;

    @Autowired
    public OrdnerRepositoryIT(OrdnerRepository ordnerRepository) {
        this.ordnerRepository = ordnerRepository;
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenOrdnerAreInserted() {
        // DESCRIBE & ACT
        long actual = ordnerRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}

