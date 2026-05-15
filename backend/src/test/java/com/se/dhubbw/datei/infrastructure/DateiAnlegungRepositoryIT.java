package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.DateiAnlegungRepository;
import com.se.dhubbw.shared.testing.IntegrationTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
public class DateiAnlegungRepositoryIT {
    private final DateiAnlegungRepository dateiAnlegungRepository;

    @Autowired
    public DateiAnlegungRepositoryIT(
            DateiAnlegungRepository dateiAnlegungRepository) {
        this.dateiAnlegungRepository = dateiAnlegungRepository;
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenDateiAnlegungAreInserted() {
        // DESCRIBE & ACT
        long actual = dateiAnlegungRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}

