package com.se.dhubbw.student.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO for registration request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RegisterRequestDTO {

    /**
     * DHBW email address.
     */
    @Getter
    private String dhbwMail;

    /**
     * Name of the student. Has to match the following format:
     * "FirstName LastName"
     */
    private String name;

    /**
     * Password.
     */
    private String password;
}
