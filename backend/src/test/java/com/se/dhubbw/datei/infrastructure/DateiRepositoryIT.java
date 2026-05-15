package com.se.dhubbw.datei.infrastructure;

import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.shared.testing.IntegrationTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@IntegrationTest
public class DateiRepositoryIT {
    private final DateiRepository dateiRepository;

    @Autowired
    public DateiRepositoryIT(DateiRepository dateiRepository) {
        this.dateiRepository = dateiRepository;
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenMealsAreInserted() {
        // DESCRIBE & ACT
        long actual = dateiRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }

    @Test
    public void givenDataSQL_whenApplicationStarts_thenPNGFileInserted() {
        // DESCRIBE & ACT
        List<Datei> actual = dateiRepository.findAll();
        // ASSERT
        actual.forEach(file -> Assertions.assertNotNull(file.getData()));
    }
}