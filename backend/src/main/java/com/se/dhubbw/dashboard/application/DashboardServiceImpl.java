package com.se.dhubbw.dashboard.application;

import com.se.dhubbw.dashboard.domain.DashboardService;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.mensa.domain.MensaService;
import com.se.dhubbw.mensa.infrastructure.Mensa;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for aggregating dashboard data.
 *
 * <p>Delegates to the mensa and datei modules to supply the data
 * required by the frontend dashboard: today's meals and the latest
 * files visible to the authenticated user.</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    /**
     * Maximum number of files returned by the dashboard endpoints.
     */
    private static final int DASHBOARD_LIMIT = 5;

    /**
     * Service for querying mensa meal data.
     */
    private final MensaService mensaService;

    /**
     * Repository for looking up students by email.
     */
    private final StudentRepository studentRepository;

    /**
     * Repository for querying course enrollment requests.
     */
    private final KursBeitrittsAnfrageRepository kursBeitrittsAnfrageRepository;

    /**
     * Repository for querying file data.
     */
    private final DateiRepository dateiRepository;

    /** {@inheritDoc} */
    @Override
    public List<Mensa> getMealsForToday() {
        return mensaService.getMealsForDay(LocalDate.now());
    }

    /** {@inheritDoc} */
    @Override
    public List<Datei> getAllLatestDateien() {
        return dateiRepository.findAllOrderByZuletztBearbeitetDesc()
            .stream().limit(DASHBOARD_LIMIT).toList();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<List<Datei>> getLatestDateienForStudent(
        final String dhbwMail) {
        Optional<Student> student =
            studentRepository.findByDhbwMail(dhbwMail);
        if (student.isEmpty()) {
            return Optional.empty();
        }
        Optional<KursBeitrittsAnfrage> anfrage =
            kursBeitrittsAnfrageRepository
                .findByStudentAndIsApprovedTrue(student.get());
        if (anfrage.isEmpty()) {
            return Optional.empty();
        }
        UUID kursId = anfrage.get().getKurs().getKursID();
        return Optional.of(
            dateiRepository
                .findByKursIdOrderByZuletztBearbeitetDesc(kursId)
                .stream().limit(DASHBOARD_LIMIT).toList()
        );
    }
}
