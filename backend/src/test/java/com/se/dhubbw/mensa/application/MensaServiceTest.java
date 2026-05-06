package com.se.dhubbw.mensa.application;

import com.se.dhubbw.mensa.infrastructure.Mensa;
import com.se.dhubbw.mensa.infrastructure.MensaRepository;
import com.se.dhubbw.mensa.infrastructure.MensaScraper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MensaServiceTest {

    @Mock
    private MensaRepository mensaRepository;

    @Mock
    private MensaScraper mensaScraper;

    @InjectMocks
    private MensaService mensaService;

    @Test
    public void givenEmptyScrapedList_whenSync_thenSkipsAllRepoOperations() {
        when(mensaScraper.scrapeCurrentWeek()).thenReturn(Collections.emptyList());

        mensaService.sync();

        verify(mensaRepository, never()).deleteBeforeDate(any());
        verify(mensaRepository, never()).deleteByDatum(any());
        verify(mensaRepository, never()).saveAll(any());
    }

    @Test
    public void givenScrapedMeals_whenSync_thenDeletesOldCutoffAndSavesNew() {
        Mensa meal = new Mensa();
        meal.setDatum(LocalDate.of(2026, 5, 6));
        when(mensaScraper.scrapeCurrentWeek()).thenReturn(List.of(meal));

        mensaService.sync();

        verify(mensaRepository).deleteBeforeDate(any(LocalDate.class));
        verify(mensaRepository).deleteByDatum(LocalDate.of(2026, 5, 6));
        verify(mensaRepository).saveAll(List.of(meal));
    }

    @Test
    public void givenMealsOnTwoDates_whenSync_thenDeletesAndSavesPerDate() {
        Mensa meal1 = new Mensa();
        meal1.setDatum(LocalDate.of(2026, 5, 6));
        Mensa meal2 = new Mensa();
        meal2.setDatum(LocalDate.of(2026, 5, 6));
        Mensa meal3 = new Mensa();
        meal3.setDatum(LocalDate.of(2026, 5, 7));
        when(mensaScraper.scrapeCurrentWeek())
            .thenReturn(List.of(meal1, meal2, meal3));

        mensaService.sync();

        verify(mensaRepository, times(2)).deleteByDatum(any(LocalDate.class));
        verify(mensaRepository, times(2)).saveAll(any());
    }

    @Test
    public void givenDate_whenGetMealsForDay_thenDelegatesToRepository() {
        LocalDate date = LocalDate.of(2026, 5, 6);
        List<Mensa> expected = List.of(new Mensa());
        when(mensaRepository.findByDatum(date)).thenReturn(expected);

        List<Mensa> result = mensaService.getMealsForDay(date);

        assertEquals(expected, result);
        verify(mensaRepository).findByDatum(date);
    }

    @Test
    public void givenScheduledSync_whenCalled_thenDelegatesToSync() {
        when(mensaScraper.scrapeCurrentWeek()).thenReturn(Collections.emptyList());

        mensaService.scheduledSync();

        verify(mensaScraper).scrapeCurrentWeek();
    }
}
