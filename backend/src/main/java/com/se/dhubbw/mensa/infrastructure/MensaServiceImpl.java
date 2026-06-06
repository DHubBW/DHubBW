package com.se.dhubbw.mensa.infrastructure;

import com.se.dhubbw.mensa.domain.MensaService;
import com.se.dhubbw.mensa.infrastructure.db.Mensa;
import com.se.dhubbw.mensa.infrastructure.db.MensaRepository;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link MensaService} interface.
 * Performs the daily synchronization of the mensa meal plan and
 * provides queries.
 *
 * <p>Runs a daily scheduled scrape of the Studierendenwerk Karlsruhe
 * meal plan and persists the results. Existing records for a given date
 * are replaced on every sync so the data always reflects the latest state.
 *
 * @architecture.component Mensaverwaltung
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with JPA access and
 *     scheduling)
 */
@Service
@RequiredArgsConstructor
public class MensaServiceImpl implements MensaService {

    /**
     * Number of weeks after which old meal records are purged.
     */
    private static final int RETENTION_WEEKS = 4;

    /**
     * Number of days in a work week (Monday to Friday).
     */
    private static final int WORKWEEK_DAYS = 5;

    /**
     * Repository for persisting and querying meal entities.
     */
    private final MensaRepository mensaRepository;

    /**
     * Scraper that fetches meals from the external mensa website.
     */
    private final MensaScraperComponent mensaScraperComponent;

    /**
     * Logger instance.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(MensaServiceImpl.class);

    /** {@inheritDoc} */
    @Override
    @Transactional
    @Scheduled(cron = "0 0 11 * * *", zone = "Europe/Berlin")
    public void scheduledSync() {
        sync();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void sync() {
        List<Mensa> scraped = mensaScraperComponent.scrapeCurrentWeek();
        if (scraped.isEmpty()) {
            colorLogger.colorError("No meals scraped - skipping sync");
            return;
        }

        LocalDate cutoff = LocalDate.now().minusWeeks(RETENTION_WEEKS);
        mensaRepository.deleteBeforeDate(cutoff);

        Map<LocalDate, List<Mensa>> byDate = scraped.stream()
            .collect(Collectors.groupingBy(Mensa::getDatum));

        for (Map.Entry<LocalDate, List<Mensa>> entry : byDate.entrySet()) {
            mensaRepository.deleteByDatum(entry.getKey());
            mensaRepository.saveAll(entry.getValue());
        }

        colorLogger.colorInfo(
            "Mensa sync complete: {} meals across {} days",
            scraped.size(),
            byDate.size());
    }

    /** {@inheritDoc} */
    @Override
    public List<Mensa> getMealsForDay(final LocalDate datum) {
        return mensaRepository.findByDatum(datum);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, List<Mensa>> getMealsForCurrentWeek() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return getMealsForWeekStartingOn(monday);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, List<Mensa>> getMealsForNextWeek() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.with(
                TemporalAdjusters.next(DayOfWeek.MONDAY));
        return getMealsForWeekStartingOn(nextMonday);
    }

    /**
     * Retrieve meals for a specific week starting on the given Monday.
     *
     * @param monday the Monday of the target week
     * @return map with weekday keys (Mo, Di, Mi, Do, Fr) and meal lists
     */
    private Map<String, List<Mensa>> getMealsForWeekStartingOn(
            final LocalDate monday) {
        Map<String, List<Mensa>> weekMap = new LinkedHashMap<>();

        weekMap.put("Mo", mensaRepository.findByDatum(monday));
        weekMap.put("Di", mensaRepository.findByDatum(monday.plusDays(1)));
        weekMap.put("Mi", mensaRepository.findByDatum(monday.plusDays(2)));
        weekMap.put("Do",
                mensaRepository.findByDatum(monday.plusDays(
                        WORKWEEK_DAYS - 2)));
        weekMap.put("Fr",
                mensaRepository.findByDatum(monday.plusDays(
                        WORKWEEK_DAYS - 1)));

        return weekMap;
    }
}
