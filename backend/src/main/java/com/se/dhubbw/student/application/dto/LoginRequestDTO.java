package com.se.dhubbw.student.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO for login request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LoginRequestDTO {

    /**
     * DHBW email address.
     */
    @Getter
    private String dhbwMail;

    /**
     * Password.
     */
    private String password;
}
