package com.se.dhubbw.student.application.dto;

/**
 * DTO for registration request.
 * @param dhbwMail DHBW email address.
 * @param name Name of the student. Has to match the following format:
 * 'FirstName LastName'
 * @param password Password.
 */
public record RegisterRequestDTO(
    String dhbwMail, String name, String password) { }
