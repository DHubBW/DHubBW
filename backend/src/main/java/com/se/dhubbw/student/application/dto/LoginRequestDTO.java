package com.se.dhubbw.student.application.dto;

/**
 * Data transfer object for login requests.
 * Carries email and password from client to server.
 *
 * @architecture.component Benutzerverwaltung
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation HTTP
 *     Request → Service)
 * @param dhbwMail DHBW email address.
 * @param password Password.
 */
public record LoginRequestDTO(String dhbwMail, String password) { }
