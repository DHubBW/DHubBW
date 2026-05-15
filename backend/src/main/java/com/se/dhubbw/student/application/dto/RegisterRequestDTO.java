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
     * First name.
     */
    private String vorname;

    /**
     * Last name.
     */
    private String nachname;

    /**
     * Password.
     */
    private String password;
}
