package com.se.dhubbw.kurs.application.dto;

import com.se.dhubbw.student.infrastructure.util.StudentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing a course member.
 *
 * <p>Carries the data a course administrator needs when listing
 * the members of their course: the student's identifier, display
 * name, DHBW email address, and current role.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KursmemberDTO {

    /**
     * Unique identifier of the student.
     */
    private UUID studentID;

    /**
     * Full name of the student.
     */
    private String name;

    /**
     * DHBW email address of the student.
     */
    private String dhbwMail;

    /**
     * Current role of the student.
     */
    private StudentType studentType;
}
