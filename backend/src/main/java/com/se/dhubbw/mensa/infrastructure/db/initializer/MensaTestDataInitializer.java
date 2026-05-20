package com.se.dhubbw.mensa.infrastructure.db.initializer;

import com.se.dhubbw.mensa.infrastructure.db.Mensa;
import com.se.dhubbw.mensa.infrastructure.db.MensaRepository;
import com.se.dhubbw.mensa.infrastructure.util.Ernaehrung;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Initializes dynamic mensa test data for current and next week.
 * Only runs in dev profile.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class MensaTestDataInitializer implements CommandLineRunner {

    /**
     * Number of workweek days to generate data for.
     */
    private static final int WORKWEEK_DAYS = 5;

    /**
     * Base price for meals in euros.
     */
    private static final double BASE_PRICE = 5.50;

    /**
     * Price increment per day in euros.
     */
    private static final double PRICE_INCREMENT = 0.5;

    /**
     * ColorLogger instance for logging operations.
     */
    private final ColorLogger colorLogger =
            new ColorLogger(this.getClass());

    /**
     * Repository for mensa meals.
     */
    private final MensaRepository mensaRepository;

    /**
     * Run method executed on application startup.
     *
     * <p>Initializes mensa test data for current and next week.</p>
     *
     * @param args command line arguments
     */
    @Override
    public void run(final String... args) {
        colorLogger.colorInfo(
                "Starting dynamic mensa test data initialization...");

        LocalDate today = LocalDate.now();
        LocalDate currentMonday = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextMonday = currentMonday.plusWeeks(1);

        List<Mensa> allMeals = new ArrayList<>();
        allMeals.addAll(generateWeekMeals(currentMonday, "current"));
        allMeals.addAll(generateWeekMeals(nextMonday, "next"));

        mensaRepository.saveAll(allMeals);

        colorLogger.colorInfo("Successfully initialized "
                + allMeals.size() + " mensa test meals");
    }

    /**
     * Generate meals for a single week starting from Monday.
     *
     * @param monday the Monday of the week
     * @param weekLabel label for logging (current/next)
     * @return list of generated meals
     */
    private List<Mensa> generateWeekMeals(
            final LocalDate monday, final String weekLabel) {
        List<Mensa> meals = new ArrayList<>();

        for (int day = 0; day < WORKWEEK_DAYS; day++) {
            LocalDate date = monday.plusDays(day);
            meals.add(createMeal(date, Ernaehrung.FLEISCH,
                    "Schnitzel mit Pommes", day));
            meals.add(createMeal(date, Ernaehrung.VEGETARISCH,
                    "Vegetarische Lasagne", day));
            meals.add(createMeal(date, Ernaehrung.VEGAN,
                    "Gemüse-Curry mit Reis", day));
        }

        colorLogger.colorDebug("Generated " + meals.size()
                + " meals for " + weekLabel + " week starting "
                + monday);

        return meals;
    }

    /**
     * Create a single mensa meal.
     *
     * @param date the date of the meal
     * @param ernaehrung the type of meal
     * @param baseName base name for the meal
     * @param dayOffset day offset for variation
     * @return the created meal
     */
    private Mensa createMeal(final LocalDate date,
                             final Ernaehrung ernaehrung,
                             final String baseName,
                             final int dayOffset) {
        Mensa meal = new Mensa();
        meal.setDatum(date);
        meal.setErnaehrung(ernaehrung);
        meal.setEssensbezeichnung(baseName);
        meal.setAllergene("Test-Allergene");
        meal.setPreis(BASE_PRICE + dayOffset * PRICE_INCREMENT);
        return meal;
    }
}
