package com.se.dhubbw.dashboard.application;

import com.se.dhubbw.dashboard.domain.DashboardService;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.mensa.infrastructure.Mensa;
import com.se.dhubbw.shared.testing.IntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@IntegrationTest
public class DashboardServiceIT {

    private final DashboardService dashboardService;

    private final String LINUS_MAIL = "kronenberger.linus.a24@student.dhbw-karlsruhe.de";
    private final String NICO_MAIL = "anonym.nico.a24@student.dhbw-karlsruhe.de";

    @Autowired
    public DashboardServiceIT(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

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
            .getLatestDateienForStudent(NICO_MAIL);
        // ASSERT
        Assertions.assertTrue(actual.isEmpty());
    }

    @Test
    public void givenDataSQL_whenGetLatestDateienForApprovedStudent_thenPresent() {
        // DESCRIBE & ACT
        Optional<List<Datei>> actual = dashboardService
            .getLatestDateienForStudent(LINUS_MAIL);
        // ASSERT
        Assertions.assertTrue(actual.isPresent());
        Assertions.assertFalse(actual.get().isEmpty());
    }

    @Test
    public void givenDataSQL_whenGetLatestDateienForApprovedStudent_thenSortedDesc() {
        // DESCRIBE & ACT
        List<Datei> actual = dashboardService
            .getLatestDateienForStudent(LINUS_MAIL)
            .orElseThrow();
        // ASSERT
        for (int i = 0; i < actual.size() - 1; i++) {
          Assertions.assertFalse(
              actual.get(i).getZuletztBearbeitet().isBefore(actual.get(i + 1).getZuletztBearbeitet()));
        }
    }
}
