package com.se.dhubbw.dashboard.application;

import com.se.dhubbw.dashboard.domain.DashboardService;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.mensa.infrastructure.Mensa;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Profile("dev")
public class DashboardServiceIT {

    private final DashboardService dashboardService;

    @Test
    public void givenDataSQL_whenGetAllLatestDateien_thenReturnsAll() {
        // DESCRIBE & ACT
        List<Datei> actual = dashboardService.getAllLatestDateien();
        // ASSERT
        Assertions.assertFalse(actual.isEmpty());
    }

    @Test
    public void givenDataSQL_whenGetMealsForToday_thenReturnsNotNull() {
        // DESCRIBE & ACT
        List<Mensa> actual = dashboardService.getMealsForToday();
        // ASSERT
        Assertions.assertNotNull(actual);
    }

    @Test
    public void givenDataSQL_whenGetLatestDateienForUnknownStudent_thenEmpty() {
        // DESCRIBE & ACT
        Optional<List<Datei>> actual = dashboardService
            .getLatestDateienForStudent("unbekannt@dhbw.de");
        // ASSERT
        Assertions.assertTrue(actual.isEmpty());
    }

    @Test
    public void givenDataSQL_whenGetLatestDateienForUnapprovedStudent_thenEmpty() {
        // DESCRIBE & ACT
        Optional<List<Datei>> actual = dashboardService
            .getLatestDateienForStudent("nico.anonym@dhbw-karlsruhe.de");
        // ASSERT
        Assertions.assertTrue(actual.isEmpty());
    }

    @Test
    public void givenDataSQL_whenGetLatestDateienForApprovedStudent_thenPresent() {
        // DESCRIBE & ACT
        Optional<List<Datei>> actual = dashboardService
            .getLatestDateienForStudent("linus.kronenberger@dhbw-karlsruhe.de");
        // ASSERT
        Assertions.assertTrue(actual.isPresent());
        Assertions.assertFalse(actual.get().isEmpty());
    }

    @Test
    public void givenDataSQL_whenGetLatestDateienForApprovedStudent_thenSortedDesc() {
        // DESCRIBE & ACT
        List<Datei> actual = dashboardService
            .getLatestDateienForStudent("linus.kronenberger@dhbw-karlsruhe.de")
            .orElseThrow();
        // ASSERT
        for (int i = 0; i < actual.size() - 1; i++) {
            Assertions.assertTrue(
                !actual.get(i).getZuletztBearbeitet()
                    .isBefore(actual.get(i + 1).getZuletztBearbeitet())
            );
        }
    }
}
