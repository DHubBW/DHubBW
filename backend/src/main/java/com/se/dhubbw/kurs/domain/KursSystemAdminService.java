package com.se.dhubbw.kurs.domain;

import com.se.dhubbw.kurs.infrastructure.db.KursErstellungsAnfrage;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for system-admin course creation request management.
 *
 * <p>Provides operations for a system administrator to list,
 * approve, or decline pending course creation requests.</p>
 */
public interface KursSystemAdminService {

    /**
     * Retrieve all pending (not yet approved) course creation
     * requests.
     *
     * @return list of pending creation requests; may be empty
     */
    List<KursErstellungsAnfrage> retrieveKursErstellungsanfragen();

    /**
     * Approve a pending course creation request.
     *
     * <p>Creates the new course, promotes the requesting student
     * to {@code KURSADMIN}, and logs course membership via an
     * approved enrollment request.</p>
     *
     * @param anfrageId the ID of the request to approve
     * @throws java.util.NoSuchElementException if the request is not found
     * @throws IllegalStateException if the requesting student is
     *     already enrolled in a course
     * @throws IllegalArgumentException if a course with the same
     *     name and cohort year already exists
     */
    void approveKursErstellungsanfrage(UUID anfrageId);

    /**
     * Decline and permanently delete a pending course creation
     * request.
     *
     * @param anfrageId the ID of the request to decline
     * @throws java.util.NoSuchElementException if the request is not found
     */
    void declineKursErstellungsanfrage(UUID anfrageId);
}
