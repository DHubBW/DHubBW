package com.se.dhubbw.mensa.application;

import com.se.dhubbw.mensa.infrastructure.db.Mensa;
import com.se.dhubbw.mensa.infrastructure.db.MensaRepository;
import com.se.dhubbw.mensa.infrastructure.MensaScraperComponent;
import com.se.dhubbw.mensa.infrastructure.MensaServiceImpl;
import com.se.dhubbw.shared.testing.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class MensaServiceTest {

    @Mock
    private MensaRepository mensaRepository;

    @Mock
    private MensaScraperComponent mensaScraperComponent;

    @InjectMocks
    private MensaServiceImpl mensaService;

    @Test
    public void givenEmptyScrapedList_whenSync_thenSkipsAllRepoOperations() {
        when(mensaScraperComponent.scrapeCurrentWeek())
                .thenReturn(Collections.emptyList());

        mensaService.sync();

        verify(mensaRepository, never()).deleteBeforeDate(any());
        verify(mensaRepository, never()).deleteByDatum(any());
        verify(mensaRepository, never()).saveAll(any());
    }

    @Test
    public void givenScrapedMeals_whenSync_thenDeletesOldCutoffAndSavesNew() {
        Mensa meal = new Mensa();
        meal.setDatum(LocalDate.of(2026, 5, 6));
        when(mensaScraperComponent.scrapeCurrentWeek()).thenReturn(List.of(meal));

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
        when(mensaScraperComponent.scrapeCurrentWeek())
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
        when(mensaScraperComponent.scrapeCurrentWeek())
                .thenReturn(Collections.emptyList());

        mensaService.scheduledSync();

        verify(mensaScraperComponent).scrapeCurrentWeek();
    }

    @Test
    public void
            givenCurrentWeek_whenGetMealsForCurrentWeek_thenReturnsMapWithFiveWeekdays() {
        LocalDate monday = LocalDate.now().with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        when(mensaRepository.findByDatum(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        Map<String, List<Mensa>> result =
                mensaService.getMealsForCurrentWeek();

        assertNotNull(result);
        assertEquals(5, result.size());
        assertTrue(result.containsKey("Mo"));
        assertTrue(result.containsKey("Di"));
        assertTrue(result.containsKey("Mi"));
        assertTrue(result.containsKey("Do"));
        assertTrue(result.containsKey("Fr"));

        verify(mensaRepository).findByDatum(monday);
        verify(mensaRepository).findByDatum(monday.plusDays(1));
        verify(mensaRepository).findByDatum(monday.plusDays(2));
        verify(mensaRepository).findByDatum(monday.plusDays(3));
        verify(mensaRepository).findByDatum(monday.plusDays(4));
    }

    @Test
    public void
            givenNextWeek_whenGetMealsForNextWeek_thenReturnsMapWithFiveWeekdays() {
        LocalDate nextMonday = LocalDate.now().with(
                TemporalAdjusters.next(DayOfWeek.MONDAY));

        when(mensaRepository.findByDatum(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        Map<String, List<Mensa>> result =
                mensaService.getMealsForNextWeek();

        assertNotNull(result);
        assertEquals(5, result.size());
        assertTrue(result.containsKey("Mo"));
        assertTrue(result.containsKey("Di"));
        assertTrue(result.containsKey("Mi"));
        assertTrue(result.containsKey("Do"));
        assertTrue(result.containsKey("Fr"));

        verify(mensaRepository).findByDatum(nextMonday);
        verify(mensaRepository).findByDatum(nextMonday.plusDays(1));
        verify(mensaRepository).findByDatum(nextMonday.plusDays(2));
        verify(mensaRepository).findByDatum(nextMonday.plusDays(3));
        verify(mensaRepository).findByDatum(nextMonday.plusDays(4));
    }

    @Test
    public void givenMealsExist_whenGetCurrentWeek_thenReturnsMealsInMap() {
        LocalDate monday = LocalDate.now().with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        Mensa mondayMeal = new Mensa();
        mondayMeal.setDatum(monday);
        Mensa tuesdayMeal = new Mensa();
        tuesdayMeal.setDatum(monday.plusDays(1));

        when(mensaRepository.findByDatum(monday))
                .thenReturn(List.of(mondayMeal));
        when(mensaRepository.findByDatum(monday.plusDays(1)))
                .thenReturn(List.of(tuesdayMeal));
        when(mensaRepository.findByDatum(monday.plusDays(2)))
                .thenReturn(Collections.emptyList());
        when(mensaRepository.findByDatum(monday.plusDays(3)))
                .thenReturn(Collections.emptyList());
        when(mensaRepository.findByDatum(monday.plusDays(4)))
                .thenReturn(Collections.emptyList());

        Map<String, List<Mensa>> result =
                mensaService.getMealsForCurrentWeek();

        assertEquals(1, result.get("Mo").size());
        assertEquals(mondayMeal, result.get("Mo").get(0));
        assertEquals(1, result.get("Di").size());
        assertEquals(tuesdayMeal, result.get("Di").get(0));
    }
}
