package com.se.dhubbw.kurs.domain;

import com.se.dhubbw.kurs.application.dto.output.KursanfrageDTO;

import java.util.List;
import java.util.UUID;

/**
 * Domain interface for course admin enrollment request management.
 * Defines operations for listing, approving, and declining enrollment requests.
 *
 * <p>Provides operations for a course administrator to list,
 * approve, or decline pending join requests for their course.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Domain Layer
 * @architecture.category A-Software (pure domain interface, technology-free)
 */
public interface KursAdminService {

    /**
     * Retrieve all pending join requests for the course
     * administered by the given KURSADMIN.
     *
     * @param dhbwMail the DHBW mail address of the course admin
     * @return list of pending requests as DTOs, may be empty
     * @throws java.util.NoSuchElementException if the admin student
     *     or their course assignment is not found
     */
    List<KursanfrageDTO> retrieveKursbeitrittsanfragen(String dhbwMail);

    /**
     * Approve a pending join request, assigning the student to
     * the admin's course.
     *
     * <p>The student must not already be enrolled in any course.
     * A student may only belong to one course at a time.</p>
     *
     * @param anfrageId the ID of the request to approve
     * @param dhbwMail the DHBW mail address of the course admin
     * @throws java.util.NoSuchElementException if the request or
     *     the admin is not found
     * @throws IllegalArgumentException if the request does not
     *     belong to the admin's course
     * @throws IllegalStateException if the student is already
     *     enrolled in another course
     */
    void approveKursbeitrittsanfrage(UUID anfrageId, String dhbwMail);

    /**
     * Decline and permanently delete a pending join request.
     *
     * @param anfrageId the ID of the request to decline
     * @param dhbwMail the DHBW mail address of the course admin
     * @throws java.util.NoSuchElementException if the request or
     *     the admin is not found
     * @throws IllegalArgumentException if the request does not
     *     belong to the admin's course
     */
    void declineKursbeitrittsanfrage(UUID anfrageId, String dhbwMail);
}
