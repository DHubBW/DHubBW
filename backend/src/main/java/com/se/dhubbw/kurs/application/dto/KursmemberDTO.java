package com.se.dhubbw.kurs.application.dto;

import com.se.dhubbw.student.infrastructure.util.StudentType;

import java.util.UUID;

/**
 * Data transfer object for a course member (course admin view).
 * Contains ID, name, email, and role for the member list.
 *
 * <p>Carries the data a course administrator needs when listing
 * the members of their course: the student's identifier, display
 * name, DHBW email address, and current role.</p>
 *
 * @architecture.component Kursverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation
 *     Entity → API Response)
 * @param studentID Unique identifier of the student.
 * @param name Full name of the student.
 * @param dhbwMail DHBW email address of the student.
 * @param studentType Current role of the student.
 */
public record KursmemberDTO(UUID studentID,
                            String name,
                            String dhbwMail,
                            StudentType studentType) { }
