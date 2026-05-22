package com.se.dhubbw.kurs.application.dto;

import com.se.dhubbw.student.infrastructure.util.StudentType;

import java.util.UUID;

/**
 * DTO representing a course member.
 *
 * <p>Carries the data a course administrator needs when listing
 * the members of their course: the student's identifier, display
 * name, DHBW email address, and current role.</p>
 * @param studentID Unique identifier of the student.
 * @param name Full name of the student.
 * @param dhbwMail DHBW email address of the student.
 * @param studentType Current role of the student.
 */
public record KursmemberDTO(UUID studentID,
                            String name,
                            String dhbwMail,
                            StudentType studentType) { }
