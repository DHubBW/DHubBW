package com.se.dhubbw.datei.infrastructure;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Profile("dev")
public class DateiAnlegungRepositoryIT {
    private final DateiAnlegungRepository dateiAnlegungRepository;

    @Test
    public void givenDataSQL_whenApplicationStarts_thenDateiAnlegungAreInserted() {
        // DESCRIBE & ACT
        long actual = dateiAnlegungRepository.count();
        // ASSERT
        Assertions.assertTrue(actual > 0);
    }
}

