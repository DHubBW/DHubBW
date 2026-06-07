package com.se.dhubbw.shared.application.dto.output;

import com.se.dhubbw.shared.application.dto.util.PersistenceOperation;

/**
 * Data transfer object for HTTP responses on database-modifying operations.
 * Contains a status message and the type of persistence operation performed.
 *
 * @architecture.component Querschnitt (Security)
 * @architecture.layer Application Layer
 * @architecture.category R-Software (representation transformation for
 *     API Response)
 * @param response
 * @param persistenceOperation
 */
public record RequestStatusResponseDTO(
    String response,
    PersistenceOperation persistenceOperation) {
}
