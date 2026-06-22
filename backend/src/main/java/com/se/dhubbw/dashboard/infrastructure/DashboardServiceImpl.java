package com.se.dhubbw.dashboard.infrastructure;

import com.se.dhubbw.dashboard.domain.DashboardService;
import com.se.dhubbw.datei.application.dto.output.DateiDTO;
import com.se.dhubbw.datei.application.dto.output.TagDTO;
import com.se.dhubbw.datei.infrastructure.db.Datei;
import com.se.dhubbw.datei.infrastructure.db.DateiRepository;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import com.se.dhubbw.mensa.domain.MensaService;
import com.se.dhubbw.mensa.infrastructure.db.Mensa;
import com.se.dhubbw.student.infrastructure.db.Student;
import com.se.dhubbw.student.infrastructure.db.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the {@link DashboardService} interface.
 * Aggregates data from mensa and file modules for the frontend dashboard.
 *
 * <p>Delegates to the mensa and datei modules to supply the data
 * required by the frontend dashboard: today's meals and the latest
 * files visible to the authenticated user.</p>
 *
 * @architecture.component Dashboard-Aggregation
 * @architecture.layer Infrastructure Layer
 * @architecture.category T-Software (Spring Service with cross-module access)
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
    @Transactional(readOnly = true)
    public List<DateiDTO> getAllLatestDateien() {
        return dateiRepository.findAllOrderByZuletztBearbeitetDesc()
            .stream()
            .limit(DASHBOARD_LIMIT)
            .map(DashboardServiceImpl::toDateiDto)
            .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Optional<List<DateiDTO>> getLatestDateienForStudent(
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
                .stream()
                .limit(DASHBOARD_LIMIT)
                .map(DashboardServiceImpl::toDateiDto)
                .toList()
        );
    }

    /**
     * Map a {@link Datei} entity to its DTO representation.
     *
     * <p>Must be called within an active transaction so that
     * the {@code ordner} and {@code uploader} associations are
     * accessible without triggering a {@code LazyInitializationException}
     * or a LOB-stream error on PostgreSQL Large Objects.</p>
     *
     * @param datei the entity to convert
     * @return the corresponding {@link DateiDTO}
     */
    private static DateiDTO toDateiDto(final Datei datei) {
        String uploaderName = datei.getUploader() != null
            ? datei.getUploader().getName() : null;
        double durchschnitt = datei.getBewertungAnzahl() == 0
            ? 0.0
            : (double) datei.getBewertungSumme()
                / datei.getBewertungAnzahl();
        List<TagDTO> tagDtos = datei.getTags() == null
            ? List.of()
            : datei.getTags().stream()
                .map(t -> new TagDTO(t.getTagID(), t.getName()))
                .sorted((a, b) -> a.name().compareToIgnoreCase(
                    b.name()))
                .toList();
        return new DateiDTO(
            datei.getDateiID(),
            datei.getName(),
            datei.getZuletztBearbeitet(),
            datei.getFileSize(),
            uploaderName,
            datei.getOrdner().getOrdnerID(),
            datei.getOrdner().getName(),
            datei.getDownloadAnzahl(),
            durchschnitt,
            datei.getBewertungAnzahl(),
            tagDtos
        );
    }
}
