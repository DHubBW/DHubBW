package com.se.dhubbw.student.application.dto;

/**
 * DTO for login request.
 * @param dhbwMail DHBW email address.
 * @param password Password.
 */
public record LoginRequestDTO(String dhbwMail, String password) { }
