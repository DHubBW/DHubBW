package com.se.dhubbw.kurs.infrastructure;

import com.se.dhubbw.kurs.application.dto.KursanfrageDTO;
import com.se.dhubbw.kurs.domain.KursAdminService;
import com.se.dhubbw.kurs.infrastructure.db.Kurs;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrage;
import com.se.dhubbw.kurs.infrastructure.db.KursBeitrittsAnfrageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Implementation of course-admin enrollment request management.
 *
 * <p>Provides operations for a course administrator to list,
 * approve, or decline pending join requests for their course.
 * The admin's course is determined by their own approved
 * {@link KursBeitrittsAnfrage}.</p>
 */
@Service
@RequiredArgsConstructor
public class KursAdminServiceImpl implements KursAdminService {

    /**
     * Repository for enrollment request operations.
     */
    private final KursBeitrittsAnfrageRepository
        kursBeitrittsAnfrageRepository;

    /**
     * Shared helper for student and course lookups.
     */
    private final KursStudentHelper kursStudentHelper;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<KursanfrageDTO> retrieveKursbeitrittsanfragen(
            final String dhbwMail) {
        Kurs kurs = kursStudentHelper.findKursForAdminByEmail(dhbwMail);
        return kursBeitrittsAnfrageRepository
            .findByKursAndIsApprovedFalse(kurs).stream()
            .map(KursAdminServiceImpl::toDto)
            .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void approveKursbeitrittsanfrage(
            final UUID anfrageId,
            final String dhbwMail) {
        Kurs kurs = kursStudentHelper.findKursForAdminByEmail(dhbwMail);
        KursBeitrittsAnfrage anfrage = findAnfrage(anfrageId);
        verifyBelongsToCourse(anfrage, kurs);
        verifyStudentNotEnrolled(anfrage);
        anfrage.setApproved(true);
        kursBeitrittsAnfrageRepository.save(anfrage);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void declineKursbeitrittsanfrage(
            final UUID anfrageId,
            final String dhbwMail) {
        Kurs kurs = kursStudentHelper.findKursForAdminByEmail(dhbwMail);
        KursBeitrittsAnfrage anfrage = findAnfrage(anfrageId);
        verifyBelongsToCourse(anfrage, kurs);
        kursBeitrittsAnfrageRepository.delete(anfrage);
    }

    /**
     * Find a join request by its identifier.
     *
     * @param anfrageId the request ID
     * @return the matching request
     * @throws NoSuchElementException if no request with that ID exists
     */
    private KursBeitrittsAnfrage findAnfrage(final UUID anfrageId) {
        return kursBeitrittsAnfrageRepository
            .findById(anfrageId)
            .orElseThrow(() -> new NoSuchElementException(
                "Anfrage not found: " + anfrageId));
    }

    /**
     * Assert that a request belongs to the given course.
     *
     * @param anfrage the request to verify
     * @param kurs the expected course
     * @throws ResponseStatusException 403 if the request targets a
     *     different course
     */
    private void verifyBelongsToCourse(
            final KursBeitrittsAnfrage anfrage,
            final Kurs kurs) {
        if (!anfrage.getKurs().getKursID()
                .equals(kurs.getKursID())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Anfrage does not belong to admin course");
        }
    }

    /**
     * Assert that the requesting student is not already enrolled
     * in any course.
     *
     * @param anfrage the request whose student is to be checked
     * @throws IllegalStateException if the student already has an
     *     approved enrollment
     */
    private void verifyStudentNotEnrolled(
            final KursBeitrittsAnfrage anfrage) {
        if (kursBeitrittsAnfrageRepository
                .existsByStudentAndIsApprovedTrue(anfrage.getStudent())) {
            throw new IllegalStateException(
                "Student is already enrolled in a course");
        }
    }

    /**
     * Map a {@link KursBeitrittsAnfrage} entity to its DTO.
     *
     * @param anfrage the entity to convert
     * @return the corresponding {@link KursanfrageDTO}
     */
    private static KursanfrageDTO toDto(
            final KursBeitrittsAnfrage anfrage) {
        return new KursanfrageDTO(
            anfrage.getAnfrageID(),
            anfrage.getKurs().getKursID(),
            anfrage.getKurs().getKursName(),
            anfrage.getStudent().getStudentID(),
            anfrage.getStudent().getName(),
            anfrage.getAnfrageDatum()
        );
    }
}
