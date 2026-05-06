package com.se.dhubbw.mensa.application;

import com.se.dhubbw.mensa.infrastructure.Mensa;
import com.se.dhubbw.mensa.infrastructure.MensaRepository;
import com.se.dhubbw.mensa.infrastructure.MensaScraper;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service for synchronising mensa meal data.
 *
 * <p>Runs a daily scheduled scrape of the Studierendenwerk Karlsruhe
 * meal plan and persists the results. Existing records for a given date
 * are replaced on every sync so the data always reflects the latest state.
 */
@Service
@RequiredArgsConstructor
public class MensaService {

    /**
     * Number of weeks after which old meal records are purged.
     */
    private static final int RETENTION_WEEKS = 4;

    /**
     * Repository for persisting and querying meal entities.
     */
    private final MensaRepository mensaRepository;

    /**
     * Scraper that fetches meals from the external mensa website.
     */
    private final MensaScraper mensaScraper;

    /**
     * Logger instance.
     */
    private final ColorLogger colorLogger = new ColorLogger(MensaService.class);

    /**
     * Trigger a mensa sync automatically every day at 11:00.
     */
    @Transactional
    @Scheduled(cron = "0 0 11 * * *", zone = "Europe/Berlin")
    public void scheduledSync() {
        sync();
    }

    /**
     * Fetch the current meal plan and persist it, overwriting any existing
     * records for the same dates. Meals older than four weeks are removed.
     *
     * <p>Groups scraped meals by date and replaces each date's records
     * atomically within a transaction.
     */
    @Transactional
    public void sync() {
        List<Mensa> scraped = mensaScraper.scrapeCurrentWeek();
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

    /**
     * Retrieve all stored meals for the given date.
     *
     * @param datum the date to query
     * @return list of meals, may be empty
     */
    public List<Mensa> getMealsForDay(final LocalDate datum) {
        return mensaRepository.findByDatum(datum);
    }
}
