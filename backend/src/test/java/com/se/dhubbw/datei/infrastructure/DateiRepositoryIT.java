package com.se.dhubbw.datei.infrastructure;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

import java.util.List;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Profile("dev")
public class DateiRepositoryIT {
    private final DateiRepository dateiRepository;

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
        actual.forEach(file -> {
            Assertions.assertNotNull(file.getData());
        });
    }
}