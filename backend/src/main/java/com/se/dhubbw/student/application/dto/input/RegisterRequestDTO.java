package com.se.dhubbw.student.application.dto.input;

/**
 * Data transfer object for registration requests.
 * Carries email, name, and password from client to server.
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation HTTP
 *     Request → Service)
 * @param dhbwMail DHBW email address.
 * @param name Name of the student. Has to match the following format:
 * 'FirstName LastName'
 * @param password Password.
 */
public record RegisterRequestDTO(
    String dhbwMail, String name, String password) { }
