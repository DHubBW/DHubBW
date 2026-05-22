package com.se.dhubbw.kurs.domain;

import com.se.dhubbw.kurs.application.dto.KursAdminStatsDTO;
import com.se.dhubbw.kurs.application.dto.KursmemberDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for course member and role management.
 *
 * <p>Provides operations for a KURSADMIN to retrieve member
 * statistics, list all members, manage KURSADMIN roles, and
 * remove members from their course.</p>
 */
public interface KursMitgliederService {

    /**
     * Retrieve aggregated statistics for the admin's course.
     *
     * @param dhbwMail the DHBW mail address of the course admin
     * @return a {@link KursAdminStatsDTO} with member count,
     *     file count, and open request count
     * @throws java.util.NoSuchElementException if the admin student or
     *     their course assignment is not found
     */
    KursAdminStatsDTO retrieveKursAdminStats(String dhbwMail);

    /**
     * Retrieve all members of the admin's course.
     *
     * @param dhbwMail the DHBW mail address of the course admin
     * @return list of course members as DTOs
     * @throws java.util.NoSuchElementException if the admin student or
     *     their course assignment is not found
     */
    List<KursmemberDTO> retrieveAllCourseMembers(String dhbwMail);

    /**
     * Promote a course member to KURSADMIN role.
     *
     * @param studentId the ID of the student to promote
     * @param dhbwMail the DHBW mail address of the acting admin
     * @throws java.util.NoSuchElementException if either student is
     *     not found or not in the same course
     * @throws IllegalArgumentException if the target is a SYSTEMADMIN
     * @throws IllegalStateException if the target is already a
     *     KURSADMIN
     */
    void addKursadmin(UUID studentId, String dhbwMail);

    /**
     * Demote a KURSADMIN to STUDENT role.
     *
     * <p>Requires at least one other KURSADMIN to remain in the
     * course.</p>
     *
     * @param studentId the ID of the admin to demote
     * @param dhbwMail the DHBW mail address of the acting admin
     * @throws java.util.NoSuchElementException if either student is
     *     not found or not in the same course
     * @throws IllegalArgumentException if the target is not a
     *     KURSADMIN
     * @throws IllegalStateException if the target is the last
     *     KURSADMIN in the course
     */
    void removeKursadmin(UUID studentId, String dhbwMail);

    /**
     * Remove a member from the course entirely.
     *
     * <p>Only STUDENT members can be removed; KURSADMIN must be
     * demoted first.</p>
     *
     * @param studentId the ID of the member to remove
     * @param dhbwMail the DHBW mail address of the acting admin
     * @throws java.util.NoSuchElementException if either student is
     *     not found or not in the same course
     * @throws IllegalStateException if the target is a KURSADMIN
     */
    void removeKursMember(UUID studentId, String dhbwMail);
}
