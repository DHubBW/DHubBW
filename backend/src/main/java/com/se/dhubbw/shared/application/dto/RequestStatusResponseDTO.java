package com.se.dhubbw.shared.application.dto;

import com.se.dhubbw.shared.application.dto.util.PersistenceOperation;

/**
 * The RequestStatusReponse is used for HTTP responses,
 * if they affect the database.
 * @param response
 * @param persistenceOperation
 */
public record RequestStatusResponseDTO(
    String response,
    PersistenceOperation persistenceOperation) {
}
